package networking;

// I/O
import java.io.Closeable;
import java.io.IOException;

/**
 * The {@link Servable} describes a resource that can be served asynchronously
 * and closed later on.
 */
public interface Servable extends Closeable {
  /**
   * Serve up the resource.
   *
   * @throws IOException In case of an I/O error.
   */
  void serve() throws IOException;
}
