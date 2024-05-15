package com.teragrep.rlp_08;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowMovingAverages;
import com.teragrep.rlp_03.channel.socket.SocketFactory;
import com.teragrep.rlp_03.channel.socket.TLSFactory;
import com.teragrep.rlp_03.eventloop.EventLoop;
import com.teragrep.rlp_03.eventloop.EventLoopFactory;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.frame.delegate.FrameContext;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;
import com.teragrep.rlp_03.server.Server;
import com.teragrep.rlp_03.server.ServerFactory;
import com.teragrep.rlp_03.channel.socket.PlainFactory;
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

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("port", "1601"));
        boolean tls = Boolean.parseBoolean(System.getProperty("tls", "false"));
        int threads = Integer.parseInt(System.getProperty("threads", "1"));
        System.out.println(
                " _____ _     _                _ _ _   _                        _ _ \n" +
                "|_   _| |__ (_)___  __      _(_) | | | | ___  ___  ___    __ _| | |\n" +
                "  | | | '_ \\| / __| \\ \\ /\\ / / | | | | |/ _ \\/ __|/ _ \\  / _` | | |\n" +
                "  | | | | | | \\__ \\  \\ V  V /| | | | | | (_) \\__ \\  __/ | (_| | | |\n" +
                "  |_| |_| |_|_|___/   \\_/\\_/ |_|_|_| |_|\\___/|___/\\___|  \\__,_|_|_|\n" +
                "                                                                   \n" +
                "                                                     _     \n" +
                " _   _  ___  _   _ _ __   _ __ ___  ___ ___  _ __ __| |___ \n" +
                "| | | |/ _ \\| | | | '__| | '__/ _ \\/ __/ _ \\| '__/ _` / __|\n" +
                "| |_| | (_) | |_| | |    | | |  __/ (_| (_) | | | (_| \\__ \\\n" +
                " \\__, |\\___/ \\__,_|_|    |_|  \\___|\\___\\___/|_|  \\__,_|___/\n" +
                " |___/                                                     \n"
        );
        System.out.println("----");
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        int metricsInterval = Integer.parseInt(System.getProperty("metricsInterval", "0"));
        Consumer<FrameContext> syslogConsumer = createSyslogConsumer(metricsInterval);
        DefaultFrameDelegate frameDelegate = new DefaultFrameDelegate(syslogConsumer);
        Supplier<FrameDelegate> frameDelegateSupplier = new Supplier<>() {
            @Override
            public FrameDelegate get() {
                return frameDelegate;
            }
        };
        EventLoopFactory eventLoopFactory = new EventLoopFactory();
        EventLoop eventLoop = eventLoopFactory.create();
        Thread eventLoopThread = new Thread(eventLoop);
        eventLoopThread.start();
        ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                executorService,
                createSocketFactory(tls),
                frameDelegateSupplier
        );
        System.out.println("Starting " + (tls ? "tls" : "plain") + "server with <" + threads + "> thread(s) at port <" + port + ">");
        Server server = serverFactory.create(port);
        Thread.sleep(Integer.MAX_VALUE);
        eventLoop.stop();
        eventLoopThread.join();
        frameDelegate.close();
        executorService.shutdown();
    }

    private static Consumer<FrameContext> createSyslogConsumer(int metricsInterval) {
        if(metricsInterval > 0) {
            MetricRegistry metricRegistry = new MetricRegistry();
            metricRegistry.register(name("total", "records"), totalRecords);
            metricRegistry.register(name("total", "bytes"), totalBytes);
            ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .build();
            reporter.start(metricsInterval, TimeUnit.SECONDS);
            System.out.println("Metrics are printed every " + metricsInterval + " seconds");
            return new Consumer<>() {
                @Override
                public synchronized void accept(FrameContext frameContext) {
                    try (RelpFrame frame = frameContext.relpFrame()) {
                        totalBytes.mark(frame.payload().size());
                        totalRecords.mark();
                    }
                }
            };
        }
        else {
            return new Consumer<>() {
                @Override
                public synchronized void accept(FrameContext frameContext) {
                }
            };
        }
    }

    private static SocketFactory createSocketFactory(boolean useTls) throws GeneralSecurityException, IOException {
        if(useTls) {
            InputStream keyStoreStream = Main.class.getClassLoader().getResourceAsStream("keystore-server.jks");
            SSLContext sslContext = SSLDemoContextFactory.authenticatedContext(keyStoreStream, "changeit", "TLSv1.3");
            Function<SSLContext, SSLEngine> sslEngineFunction = sslContext1 -> {
                SSLEngine sslEngine = sslContext1.createSSLEngine();
                sslEngine.setUseClientMode(false);
                return sslEngine;
            };
            return new TLSFactory(sslContext, sslEngineFunction);
        }
        else {
            return new PlainFactory();
        }
    }
}
