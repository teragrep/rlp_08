/*
 * Null routing relp server rlp_08
 * Copyright (C) 2023,2024  Suomen Kanuuna Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teragrep.rlp_08;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowMovingAverages;
import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.channel.socket.TLSFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.frame.delegate.FrameContext;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;

class Main {

    private static final Meter totalRecords = new Meter(new SlidingTimeWindowMovingAverages());
    private static final Meter totalBytes = new Meter(new SlidingTimeWindowMovingAverages());
    private static Consumer<FrameContext> syslogConsumer;
    private static ExecutorService executorService;
    private static EventLoop eventLoop;
    private static Supplier<FrameDelegate> frameDelegateSupplier;

    public static void main(String[] args) throws IOException, InterruptedException, GeneralSecurityException {
        int port = Integer.parseInt(System.getProperty("port", "1601"));
        boolean tls = Boolean.parseBoolean(System.getProperty("tls", "false"));
        int threads = Integer.parseInt(System.getProperty("threads", "1"));
        executorService = Executors.newFixedThreadPool(threads);
        System.out
                .println(
                        """
                                 _____ _     _                _ _ _   _                        _ _\s
                                |_   _| |__ (_)___  __      _(_) | | | | ___  ___  ___    __ _| | |
                                  | | | '_ \\| / __| \\ \\ /\\ / / | | | | |/ _ \\/ __|/ _ \\  / _` | | |
                                  | | | | | | \\__ \\  \\ V  V /| | | | | | (_) \\__ \\  __/ | (_| | | |
                                  |_| |_| |_|_|___/   \\_/\\_/ |_|_|_| |_|\\___/|___/\\___|  \\__,_|_|_|
                                                                                                  \s
                                                                                     _    \s
                                 _   _  ___  _   _ _ __   _ __ ___  ___ ___  _ __ __| |___\s
                                | | | |/ _ \\| | | | '__| | '__/ _ \\/ __/ _ \\| '__/ _` / __|
                                | |_| | (_) | |_| | |    | | |  __/ (_| (_) | | | (_| \\__ \\
                                 \\__, |\\___/ \\__,_|_|    |_|  \\___|\\___\\___/|_|  \\__,_|___/
                                 |___/                                                    \s
                                """
                );
        System.out.println("----");
        System.out.println("Listening on port " + port + " using " + threads + " threads");

        int metricsInterval = Integer.parseInt(System.getProperty("metricsInterval", "0"));
        if (metricsInterval > 0) {
            syslogConsumer = frameContext -> {
                totalBytes.mark(frameContext.relpFrame().payloadLength().toInt());
                totalRecords.mark();
            };
            MetricRegistry metricRegistry = new MetricRegistry();
            metricRegistry.register(name("total", "records"), totalRecords);
            metricRegistry.register(name("total", "bytes"), totalBytes);
            ConsoleReporter reporter = ConsoleReporter
                    .forRegistry(metricRegistry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .build();
            reporter.start(metricsInterval, TimeUnit.SECONDS);
            System.out.println("Metrics are printed every " + metricsInterval + " seconds");
        }
        else {
            syslogConsumer = frameContext -> {
            };
        }
        frameDelegateSupplier = () -> new DefaultFrameDelegate(syslogConsumer);
        EventLoopFactory eventLoopFactory = new EventLoopFactory();
        eventLoop = eventLoopFactory.create();
        Thread eventLoopThread = new Thread(eventLoop);
        eventLoopThread.start();

        if (tls) {
            System.out.println("Starting TLS server");
            tlsServer(port);
        }
        else {
            System.out.println("Starting plain server");
            plainServer(port);
        }
        try {
            eventLoopThread.join();
        }
        catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
        System.out.println("server stopped at port <" + port + ">");
        executorService.shutdown();
    }

    private static void plainServer(int port) throws IOException {
        ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                executorService,
                new PlainFactory(),
                new FrameDelegationClockFactory(frameDelegateSupplier)
        );
        serverFactory.create(port);
    }

    private static void tlsServer(int port) throws GeneralSecurityException, IOException {
        InputStream keyStoreStream = Main.class.getClassLoader().getResourceAsStream("keystore-server.jks");
        SSLContext sslContext = SSLDemoContextFactory.authenticatedContext(keyStoreStream, "changeit", "TLSv1.3");
        Function<SSLContext, SSLEngine> sslEngineFunction = sslContext1 -> {
            SSLEngine sslEngine = sslContext1.createSSLEngine();
            sslEngine.setUseClientMode(false);
            return sslEngine;
        };

        ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                executorService,
                new TLSFactory(sslContext, sslEngineFunction),
                new FrameDelegationClockFactory(frameDelegateSupplier)
        );
        serverFactory.create(port);
    }
}
