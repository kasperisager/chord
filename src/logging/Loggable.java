package logging;

// Logging
import java.util.logging.Logger;

/**
 * The {@link Loggable} interface describes an entity that can write messages to a class-wide logger.
 */
public interface Loggable {
  /**
   * Return a singleton {@link Logger} instance for the implementor.
   *
   * @return A singleton {@link Logger} instance for the implementor.
   */
  default Logger log() {
    return Logger.getLogger(this.getClass().getName());
  }
}
