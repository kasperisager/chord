package networking;

// Logging
import logging.Loggable;

// I/O
import java.io.Serializable;

/**
 * The immutable {@link Port} class describes a network port on a host.
 */
public final class Port implements Loggable, Serializable {
  /**
   * Unique ID used for serialization.
   */
  private static final long serialVersionUID = 749206610;

  /**
   * The minimum available port number.
   */
  private static final int PORT_MINIMUM = 0;

  /**
   * The maximum available port number (exclusive).
   */
  private static final int PORT_MAXIMUM = (int) Math.pow(2, 16);

  /**
   * The maximum port of the privileged area (exclusive).
   */
  private static final int PORT_PRIVILEGED = (int) Math.pow(2, 10);

  /**
   * The port number.
   */
  private final int port;

  /**
   * Initialize a new {@link Port} given an integer.
   *
   * @param port The {@link Port} number.
   */
  public Port(final int port) {
    if (port < PORT_MINIMUM || port >= PORT_MAXIMUM) {
      throw new IllegalArgumentException(
        "Port number must be between " + PORT_MINIMUM + " and "
      + (PORT_MAXIMUM - 1)
      );
    }

    if (port <= PORT_PRIVILEGED) {
      this.log().warning(
        "The specified port is within the privileged area. Root access is"
      + " required in order to bind to the port"
      );
    }

    this.port = port;
  }

  /**
   * Intialize a new {@link Port} given a {@link String} representation of an
   * integer.
   *
   * @param string The {@link String} representation of an integer.
   */
  public Port(final String port) {
    this(Integer.parseInt(port));
  }

  /**
   * Get the value of the port.
   *
   * @return The value of the port.
   */
  public int value() {
    return this.port;
  }

  @Override
  public int hashCode() {
    return this.port;
  }

  @Override
  public boolean equals(final Object object) {
    if (object == null || !(object instanceof Port)) {
      return false;
    }

    if (this == object) {
      return true;
    }

    Port port = (Port) object;

    return this.port == port.port;
  }

  @Override
  public String toString() {
    return this.port + "";
  }
}
