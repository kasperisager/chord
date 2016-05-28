package networking;

// Logging
import logging.Loggable;

// Net
import java.net.Socket;

// I/O
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The {@link Connection} class describes a connection on a channel between two processes where each process can read
 * and write objects over the connection.
 */
public final class Connection implements Loggable, Closeable {
  /**
   * The {@link Channel} that the {@link Connection} is bound to.
   */
  private final Channel channel;

  /**
   * The {@link Socket} associated with the {@link Connection}.
   */
  private final Socket socket;

  /**
   * The stream for writing objects to the remote end of the {@link Connection}.
   */
  private final ObjectInputStream in;

  /**
   * The stream for reading objects from the remote end of the {@link Connection}.
   */
  private final ObjectOutputStream out;

  /**
   * The {@link Host} on which the {@link Connection is bound.
   */
  private final Host local;

  /**
   * The {@link Host} that the {@link Connection} is connected to.
   */
  private final Host remote;

  /**
   * Initialize a new {@link Connection} for the specified {@link Socket}.
   *
   * @param socket The {@link Socket} to initialize a {@link Connection} for.
   *
   * @throws IOException In case of an I/O error.
   */
  public Connection(final Channel channel, final Socket socket) throws IOException {
    if (channel == null) {
      throw new IllegalArgumentException("A channel must be specified");
    }

    if (socket == null) {
      throw new IllegalArgumentException("A socket must be specified");
    }

    if (socket.isClosed()) {
      throw new IllegalStateException("The socket is closed");
    }

    if (!socket.isConnected()) {
      throw new IllegalStateException("The socket is not connected");
    }

    this.channel = channel;
    this.socket = socket;

    // Construct a buffered object output stream from the output stream of the socket and flush the stream. This ensures
    // that the stream header is written to the stream immediately, allowing the construction of the input stream on the
    // other end.
    this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    this.out.flush();

    // Construct a buffered object input stream from the input stream of the socket. The opposite end will by now have
    // written a stream header to the stream, which can then be read by this end of the connection.
    this.in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

    // Extract the local and remote addresses from the socket and store them.
    this.local = new Host(
      new Address(socket.getLocalAddress().getHostName()),
      new Port(socket.getLocalPort())
    );
    this.remote = new Host(
      new Address(socket.getInetAddress().getHostName()),
      new Port(socket.getPort())
    );
  }

  /**
   * Get the {@link Channel} that the {@link Connection} is bound to.
   *
   * @return The {@link Channel} that the {@link Connection} is bound to.
   */
  public Channel channel() {
    return this.channel;
  }

  /**
   * Get the {@link Host} on which the {@link Connection} is bound.
   *
   * @return The {@link Host} on which the {@link Connection is bound.
   */
  public Host local() {
    return this.local;
  }

  /**
   * Get the {@link Host} that the {@link Connection} is connected to.
   *
   * @return The {@link Host} that the {@link Connection} is connected to.
   */
  public Host remote() {
    return this.remote;
  }

  /**
   * Check if the {@link Connection} is closed.
   *
   * @return A boolean indicating whether or not the {@link Connection} is closed.
   */
  public boolean isClosed() {
    return this.socket.isClosed();
  }

  /**
   * Close the {@link Connection} between the processes.
   *
   * <p>
   * This method is idempotent and calling it more than once will therefore have no effect.
   *
   * @throws IOException In case of an I/O error.
   */
  public synchronized void close() throws IOException {
    if (this.isClosed()) {
      return;
    }

    this.socket.close();
  }

  /**
   * Read a {@link Serializable} object from the remote end of the {@link Connection}.
   *
   * @return The {@link Serializable} object read from the remote end of the {@link Connection}.
   */
  @SuppressWarnings("unchecked")
  public synchronized <T extends Serializable> T read() throws IOException {
    if (this.socket.isInputShutdown()) {
      throw new IllegalStateException("The input stream has been closed");
    }

    try {
      return (T) this.in.readObject();
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }

  /**
   * Write a {@link Serializable} object to the end of the {@link Connection}.
   *
   * @param object The object to write to the remote end of the {@link Connection}.
   * @return       The current {@link Connection} instance, for chaining.
   */
  public synchronized <T extends Serializable> Connection write(final T object) throws IOException {
    if (object == null) {
      throw new IllegalArgumentException("An object must be specified");
    }

    if (this.socket.isOutputShutdown()) {
      throw new IllegalStateException("The output stream has been closed");
    }

    this.out.writeObject(object);
    this.out.flush();

    return this;
  }
}
