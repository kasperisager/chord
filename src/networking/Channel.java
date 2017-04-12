package networking;

// Logging
import logging.Loggable;

// Threading
import threading.Worker;

// Net
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

// I/O
import java.io.Closeable;
import java.io.IOException;

// Concurrency
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The {@link Channel} class describes an asynchronous TCP abstraction for
 * working with TCP streams in a simpler manner.
 */
public final class Channel implements Loggable, Closeable {
  /**
   * The optional inbound part of the {@link Channel}.
   */
  private final Server server;

  /**
   * Initialize a new outbound {@link Channel}.
   */
  public Channel() {
    this.server = null;
  }

  /**
   * Initialize a new inbound and outbound {@link Channel} bound to the
   * specified {@link Host} and with the specified {@link Handler} for incoming
   * {@link Connection Connections}.
   *
   * @param host    The {@link Host} to bind the {@link Channel} to.
   * @param handler The {@link Handler} for incoming {@link Connection Connections}.
   *
   * @throws IOException In case of an I/O error.
   */
  public Channel(final Host host, final Handler handler) throws IOException {
    if (host == null) {
      throw new IllegalArgumentException("A host must be specified");
    }

    if (handler == null) {
      throw new IllegalArgumentException("A handler must be specified");
    }

    ServerSocket socket = new ServerSocket(host.port().value());

    this.server = new Server(socket, handler);
    this.server.start();
  }

  /**
   * Check if the {@link Channel} is closed.
   *
   * @return A boolean indicating whether or not the {@link Channel} is closed.
   */
  public boolean isClosed() {
    if (this.server == null) {
      return true;
    }

    return this.server.isClosed();
  }

  /**
   * Close the {@link Channel}, rejecting all future attempts to connect to it.
   *
   * <p>
   * This method is idempotent and calling it more than once will therefore have
   * no effect.
   *
   * @throws IOException In case of an I/O error.
   */
  public synchronized void close() throws IOException {
    if (this.isClosed()) {
      return;
    }

    this.server.close();
  }

  /**
   * Establish a {@link Connection} from the current {@link Channel} to a {@link Channel}
   * bound to the specified {@link Host} using the specified {@link Handler}.
   *
   * @param host
   * @param handler
   *
   * @throws IOException In case of an I/O error.
   */
  public void connect(final Host host, final Handler handler) throws IOException {
    if (host == null) {
      throw new IllegalArgumentException("A host must be specified");
    }

    if (handler == null) {
      throw new IllegalArgumentException("A handler must be specified");
    }

    // Extract the address and port values from the host.
    String address = host.address().value();
    int port = host.port().value();

    Socket socket = new Socket(address, port);

    new Client(socket, handler).start();
  }

  /**
   * Establsih a {@link Connection} from the current {@link Channel} to a
   * {@link Channel} bound to the specified {@link Host}, blocking until the
   * {@link Connection} has been established and then returning it.
   *
   * @param host The {@link Host} to connect to.
   * @return     The {@link Connection} to the {@link Host}.
   *
   * @throws IOException In case of an I/O error.
   */
  public Connection connect(final Host host) throws IOException {
    if (host == null) {
      throw new IllegalArgumentException("A host must be specified");
    }

    // Construct a future which can be completed once the connection to the host
    // has been established.
    CompletableFuture<Connection> future = new CompletableFuture<>();

    // Connect to the host and pass the connection to the future once connected.
    this.connect(host, future::complete);

    try {
      // Wait for the connection to be established and return it once established.
      return future.get();
    } catch (InterruptedException | ExecutionException ex) {
      return null;
    }
  }

  /**
   * The {@link Handler} interface describes a {@link Connection} handler that
   * defines the actions to be taken once a {@link Connection} has been
   * established between two {@link Host Hosts}.
   */
  @FunctionalInterface
  public interface Handler {
    /**
     * Handle an established {@link Connection}.
     *
     * @param connection The {@link Connection} to handle.
     *
     * @throws IOException In case of an I/O error.
     */
    void handle(final Connection connection) throws IOException;
  }

  private final class Server extends Worker<ServerSocket> {
    private final Handler handler;

    /**
     * Initialize a new {@link Server} on the specified {@link ServerSocket}
     * with the given {@link Handler}.
     *
     * @param socket
     * @param handler
     */
    public Server(final ServerSocket socket, final Handler handler) {
      super(socket);

      if (handler == null) {
        throw new IllegalArgumentException("A handler must be specified");
      }

      this.handler = handler;
    }

    public void run(final ServerSocket socket) {
      while (!this.isClosed()) {
        Client client = null;

        try {
          // Accept an incoming connection and construct a new client worker for
          // handling it.
          client = new Client(socket.accept(), this.handler);
          client.start();
        } catch (IOException ex1) {
          this.log().severe(ex1.toString());

          try {
            if (client != null) {
              client.close();
            }
          } catch (IOException ex2) {
            this.log().severe(ex2.toString());
          }
        }
      }
    }
  }

  private final class Client extends Worker<Socket> {
    private final Handler handler;

    /**
     * Initialize a new {@link Client} on the specified {@link Socket} with the
     * given {@link Handler}.
     *
     * @param socket
     * @param handler
     */
    public Client(final Socket socket, final Handler handler) {
      super(socket);

      if (handler == null) {
        throw new IllegalArgumentException("A handler must be specified");
      }

      this.handler = handler;
    }

    public void run(final Socket socket) throws IOException {
      // Handle the connection established over the socket.
      this.handler.handle(new Connection(Channel.this, socket));
    }
  }
}
