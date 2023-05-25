package com.teragrep.rlp_08;

import com.teragrep.rlp_03.FrameProcessor;
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.SyslogFrameProcessor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.function.Consumer;
import java.util.function.Function;

class Main {
    public static void main(String[] args) throws IOException, InterruptedException, GeneralSecurityException {
        int port = Integer.parseInt(System.getProperty("port", "1601"));
        boolean tls = Boolean.parseBoolean(System.getProperty("tls", "false"));
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
        System.out.println("Listening on port " + port + ", tls is used: " + tls);
        if (tls) {
            tlsServer(port);
        } else {
            plainServer(port);
        }
    }

    private static void plainServer(int port) throws IOException, InterruptedException {
        Consumer<byte[]> byteConsumer = bytes -> {};
        FrameProcessor syslogFrameProcessor = new SyslogFrameProcessor(byteConsumer);
        Server relpServer = new Server(port, syslogFrameProcessor);
        relpServer.start();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void tlsServer(int port) throws InterruptedException, GeneralSecurityException, IOException {
        Consumer<byte[]> byteConsumer = bytes -> {};
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
        relpServer.start();
        Thread.sleep(Long.MAX_VALUE);
    }
}
