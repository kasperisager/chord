package remote;

// Networking
import networking.Connection;
import networking.Channel;
import networking.Host;

// I/O
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

// Remote Method Invocation
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;

/**
 * The {@link Proxy} class describes an object that can be serialized and sent
 * to a remote who can then invoke the methods of the proxied object.
 *
 * @param <T> The type of object to proxy.
 */
public abstract class Proxy<T extends Serializable & Remote> implements Closeable, Remote {
  /**
   * The {@link Channel} that the {@link Proxy} uses for communication.
   */
  private final transient Channel channel;

  /**
   * Initialize a new {@link Proxy} bound to the specified {@link Host}.
   *
   * @param host The {@link Host} that the {@link Proxy} binds to.
   *
   * @throws IOException In case of an I/O error.
   */
  @SuppressWarnings("unchecked")
  public Proxy(final Host host) throws IOException {
    if (host == null) {
      throw new IllegalArgumentException("A host must be specified");
    }

    // Generate a dynamic runtime stub whose methods can be invoked remotely.
    T stub = (T) UnicastRemoteObject.exportObject(this, 0);

    // Construct a channel that will do nothing but write the generated stub to
    // the remote.
    this.channel = new Channel(host, c -> c.write(stub));
  }

  /**
   * Close the {@link Proxy} and its associated {@link Channel}.
   *
   * <p>
   * This method is idempotent and calling it more than once will therefore have
   * no effect.
   *
   * @throws IOException In case of an I/O error.
   */
  public synchronized void close() throws IOException {
    if (this.channel.isClosed()) {
      return;
    }

    this.channel.close();
  }

  /**
   * Connect to a {@link Host} and retrieve its proxied object.
   *
   * @param host The {@link Host} to connect to.
   * @param <T>  The type of the proxied object to retrieve.
   * @return     The proxied object from the remote.
   */
  public static final <T extends Serializable & Remote> T connect(final Host remote) throws IOException {
    if (remote == null) {
      throw new IllegalArgumentException("A remote host must be specified");
    }

    try (
      Channel channel = new Channel();
      Connection connection = channel.connect(remote);
    ) {
      return connection.read();
    }
  }
}
