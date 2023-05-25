package com.teragrep.rlp_08;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowMovingAverages;
import com.teragrep.rlp_03.FrameProcessor;
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.SyslogFrameProcessor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.codahale.metrics.MetricRegistry.name;

class Main {
    private static final Meter totalRecords = new Meter(new SlidingTimeWindowMovingAverages());
    private static final Meter totalBytes = new Meter(new SlidingTimeWindowMovingAverages());
    private static int threads;
    private static Consumer<byte[]> byteConsumer;
    public static void main(String[] args) throws IOException, InterruptedException, GeneralSecurityException {
        int port = Integer.parseInt(System.getProperty("port", "1601"));
        boolean tls = Boolean.parseBoolean(System.getProperty("tls", "false"));
        threads = Integer.parseInt(System.getProperty("threads", "1"));
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
        System.out.println("Listening on port " + port + " using " + threads + " threads");

        int metricsInterval = Integer.parseInt(System.getProperty("metricsInterval", "0"));
        if(metricsInterval > 0) {
            byteConsumer = bytes -> {
                totalBytes.mark(bytes.length);
                totalRecords.mark();
            };
            MetricRegistry metricRegistry = new MetricRegistry();
            metricRegistry.register(name("total", "records"), totalRecords);
            metricRegistry.register(name("total", "bytes"), totalBytes);
            ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .build();
            reporter.start(metricsInterval, TimeUnit.SECONDS);
            System.out.println("Metrics are printed every " + metricsInterval + " seconds");
        }
        else {
            byteConsumer = bytes -> {};
        }

        if (tls) {
            System.out.println("Starting TLS server");
            tlsServer(port);
        } else {
            System.out.println("Starting plain server");
            plainServer(port);
        }
    }

    private static void plainServer(int port) throws IOException, InterruptedException {
        FrameProcessor syslogFrameProcessor = new SyslogFrameProcessor(byteConsumer);
        Server relpServer = new Server(port, syslogFrameProcessor);
        relpServer.setNumberOfThreads(threads);
        relpServer.start();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void tlsServer(int port) throws InterruptedException, GeneralSecurityException, IOException {
        FrameProcessor syslogFrameProcessor = new SyslogFrameProcessor(byteConsumer);
        InputStream keyStoreStream = Main.class.getClassLoader().getResourceAsStream("keystore-server.jks");
        SSLContext sslContext;
        try {
            sslContext = SSLDemoContextFactory.authenticatedContext(keyStoreStream, "changeit", "TLSv1.3");
        } catch (IOException e) {
            throw new RuntimeException("SSL.demoContext Error: " + e);
        }
        Function<SSLContext, SSLEngine> sslEngineFunction = sslCtx -> {
            SSLEngine sslEngine = sslCtx.createSSLEngine();
            sslEngine.setUseClientMode(false);
            return sslEngine;
        };
        Server relpServer = new Server(port, syslogFrameProcessor, sslContext, sslEngineFunction);
        relpServer.setNumberOfThreads(threads);
        relpServer.start();
        Thread.sleep(Long.MAX_VALUE);
    }
}
