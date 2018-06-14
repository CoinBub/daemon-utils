package tech.coinbub.daemon;

import com.googlecode.jsonrpc4j.IJsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.coinbub.daemon.normalization.Normalized;
import tech.coinbub.daemon.proxy.ProxyUtil;

public class DaemonRunner<T> {
    private static final char[] USERNAME_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] PASSWORD_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_!*^$@.,/\\".toCharArray();
    private static final Random RANDOM = new SecureRandom();
    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonRunner.class);

    /**
     * The id of the coin, as an all-caps representation of the coin symbol. For example, BTC, LTC, ETH, etc.
     */
    private final String id;

    private Listener listener = new NoopListener();
    private JsonRpcClient client;
    private T iface;
    private Normalized<T> normalized;
    private Process process;

    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Please specify an executable path");
        }
        final String path = args[0];
        if (args.length < 2) {
            throw new IllegalArgumentException("Please specify a daemon interface");
        }
        final Class clientClass = Class.forName(args[1]);
        if (args.length < 3) {
            throw new IllegalArgumentException("Please specify a normalized daemon interface");
        }
        final Class normalizedClass = Class.forName(args[2]);
        List<String> cmd = new ArrayList<>(Arrays.asList(args));
        cmd.remove(0);
        cmd.remove(0);
        cmd.remove(0);

        new DaemonRunner(UUID.randomUUID().toString()).start(path, cmd, clientClass, normalizedClass);
    }

    public DaemonRunner() {
        this(UUID.randomUUID().toString());
    }
    public DaemonRunner(final String id) {
        this.id = id;
    }

    public void start(final String path,
            final List<String> cmd,
            final Class<T> clientClass,
            final Class<? extends Normalized> normalizedClass)
            throws IOException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            throw new RuntimeException("Windows not supported");
        }

        // Create an RPC user and password
        final String user = randomString(16, USERNAME_CHARS);
        final String password = randomString(64, PASSWORD_CHARS);

        // Build startup command
        final int port = findFreePort();
        final String[] ignored = {"rpcuser", "rpcpassword", "notify"};
        List<String> l = new ArrayList<>();
        l.add(path);
        outer: for (String part : cmd) {
            for (String ignore : ignored) {
                if (part.toLowerCase().contains(ignore)) {
                    LOGGER.warn(part + " ignored");
                    continue outer;
                }
            }
            l.add(part);
        }
        l.add("-rpcuser=" + user);
        l.add("-rpcpassword=" + password);
        l.add("-rpcport=" + port);
        
        // Build listener
        listener.prepare(l);

        // Build the process
        final ProcessBuilder builder = new ProcessBuilder(l)
                .inheritIO();
        LOGGER.info("Daemon startup command: " + String.join(" ", builder.command()));
        process = builder.start();

        // Create the client
        final URL url = new URL("http://localhost:" + port);
        LOGGER.info("Using URL {}", url.toString());
        client = new JsonRpcHttpClient(url, headers(user, password));
        iface = ProxyUtil.createClientProxy(
                this.getClass().getClassLoader(),
                clientClass,
                (IJsonRpcClient) client);
        normalized = normalizedClass.getConstructor(clientClass).newInstance(iface);

        listener.build(normalized);
    }

    public void setListener(final Listener listener) {
        this.listener = listener;
    }

    //
    // Helpers
    //

    private static Map<String, String> headers(final String username, final String password) {
        final String cred = Base64.getEncoder().encodeToString((username + ":" + password)
                .getBytes(StandardCharsets.UTF_8));
        final Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + cred);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String convertStream(final InputStream is) {
        final Scanner s = new java.util.Scanner(is, "utf8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static String randomString(final int len, final char[] chars) {
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars[RANDOM.nextInt(chars.length)]);
        }
        return sb.toString();
    }

    public static interface Listener {
        void prepare(List<String> cmd) throws IOException;
        void build(Normalized normalized) throws IOException;
        void onBlock(Observer observer);
        void onTransaction(Observer observer);
    }

    public static class StandardBitcoinListener implements Listener {
        private ServerSocket blockSocket;
        private ServerSocket walletSocket;
        private NotificationListener blockListener;
        private NotificationListener walletListener;

        @Override
        public void prepare(final List<String> cmd) throws IOException {
            blockSocket = new ServerSocket(0);
            walletSocket = new ServerSocket(0);
            blockListener = new NotificationListener(blockSocket);
            walletListener = new NotificationListener(walletSocket);

            cmd.add("-blocknotify=\"echo '%s' | nc 127.0.0.1 " + blockSocket.getLocalPort() + "\"");
            cmd.add("-walletnotify=\"echo '%s' | nc 127.0.0.1 " + walletSocket.getLocalPort() + "\"");
        }

        @Override
        public void build(final Normalized normalized) throws IOException {
            blockListener.setTransformer(new NotificationListener.BlockTransformer(normalized));
            walletListener.setTransformer(new NotificationListener.TransactionTransformer(normalized));
        }

        @Override
        public void onBlock(final Observer observer) {
            blockListener.addObserver(observer);
        }

        @Override
        public void onTransaction(final Observer observer) {
            walletListener.addObserver(observer);
        }

    }

    public static class NoopListener implements Listener {
        @Override
        public void prepare(final List<String> cmd) throws IOException {
            // no-op
            LOGGER.info("No listener factory provided");
        }

        @Override
        public void build(Normalized normalized) throws IOException {
            // no-op
        }

        @Override
        public void onBlock(final Observer observer) {
            // no-op
        }

        @Override
        public void onTransaction(final Observer observer) {
            // no-op
        }

    }

}
