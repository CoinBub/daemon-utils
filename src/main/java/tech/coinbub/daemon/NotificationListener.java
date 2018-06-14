package tech.coinbub.daemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Observable;
import java.util.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.coinbub.daemon.normalization.Normalized;

public class NotificationListener extends Observable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListener.class);
    private final ServerSocket server;
    private int port;
    private Thread listener;
    private Transformer transformer;

    public NotificationListener() throws IOException {
        this(0);
    }
    public NotificationListener(final int port) throws IOException {
        this(new ServerSocket(port));
    }
    public NotificationListener(final ServerSocket server) throws IOException {
        this.server = server;
        server.setReuseAddress(true);
        this.port = server.getLocalPort();
    }

    @Override
    public void run() {
        LOGGER.info("Listening for notifications on port {}", port);
        while (!Thread.currentThread().isInterrupted() && !server.isClosed()) {
            try (Socket connection = server.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                if ((line = reader.readLine()) != null) {
                    setChanged();;
                    notifyObservers(transform(line));
                }
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
        LOGGER.info("Listener " + Thread.currentThread().getName() + " exited");
    }

    public int getPort() {
        return server.getLocalPort();
    }

    @Override
    public synchronized void addObserver(final Observer o) {
        start();
        super.addObserver(o);
    }

    public void setTransformer(final Transformer transformer) {
        this.transformer = transformer;
    }

    public void start() {
        if (listener == null) {
            listener = new Thread(this, "notification-listener");
            listener.start();
        }
    }

    public void stop() throws IOException {
        server.close();
        interrupt();
    }

    public void interrupt() throws IOException {
        if (listener != null) {
            listener.interrupt();
        }
    }

    protected Object transform(final String line) {
        if (transformer != null) {
            return transformer.transform(line);
        }
        return line;
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }

    public static interface Transformer {
        Object transform(String line);
    }

    public static class TransactionTransformer implements Transformer {
        private final Normalized normalized;

        public TransactionTransformer(final Normalized normalized) {
            this.normalized = normalized;
        }

        @Override
        public Object transform(final String line) {
            return normalized.gettransaction(line);
        }
    }

    public static class BlockTransformer implements Transformer {
        private final Normalized normalized;

        public BlockTransformer(final Normalized normalized) {
            this.normalized = normalized;
        }

        @Override
        public Object transform(final String line) {
            return normalized.getblock(line);
        }
    }
}