package networking;

// I/O
import java.io.Serializable;

/**
 * The immutable {@link Host} class describes a combination of an {@link Address}
 * and {@link Port} tuple.
 */
public final class Host implements Serializable {
  /**
   * Unique ID used for serialization.
   */
  private static final long serialVersionUID = 130571638;

  /**
   * The default address of {@link Host Hosts}.
   */
  private static final Address DEFAULT_ADDRESS = new Address("localhost");

  /**
   * The {@link Address} of the {@link Host}.
   */
  private final Address address;

  /**
   * The {@link Port} of the {@link Host}.
   */
  private final Port port;

  /**
   * Initialize a new {@link Host} with the specified {@link Port}.
   *
   * @param port The {@link Port} of the {@link Host}.
   */
  public Host(final Port port) {
    if (port == null) {
      throw new IllegalArgumentException("A port must be specified");
    }

    this.address = DEFAULT_ADDRESS;
    this.port = port;
  }

  /**
   * Initialize a new {@link Host} with the specified {@link Address} and
   * {@link Port}.
   *
   * @param address The {@link Address} of the {@link Host}.
   * @param port    The {@link Port} of the {@link Host}.
   */
  public Host(final Address address, final Port port) {
    if (address == null) {
      throw new IllegalArgumentException("An address must be specified");
    }

    if (port == null) {
      throw new IllegalArgumentException("A port must be specified");
    }

    this.address = address;
    this.port = port;
  }

  /**
   * Initialize a new {@link Host} from a {@link String} representation.
   *
   * @param host The {@link String} representation of the {@link Host}.
   */
  public Host(final String host) {
    if (host == null) {
      throw new IllegalArgumentException("A host must be specified");
    }

    String[] parts = host.split(":");

    switch (parts.length) {
      case 1:
        this.address = DEFAULT_ADDRESS;
        this.port = new Port(parts[0]);
        break;

      case 2:
        this.address = new Address(parts[0]);
        this.port = new Port(parts[1]);
        break;

      default:
        throw new IllegalArgumentException("Invalid host specified");
    }
  }

  /**
   * Get the {@link Address} of the {@link Host}.
   *
   * @return The {@link Address} of the {@link Host}.
   */
  public Address address() {
    return this.address;
  }

  /**
   * Get the {@link Port} of the {@link Host}.
   *
   * @return The {@link Port} of the {@link Host}.
   */
  public Port port() {
    return this.port;
  }

  @Override
  public int hashCode() {
    long bits = 7L;
    bits = 31L * bits + this.address.hashCode();
    bits = 31L * bits + this.port.hashCode();

    return (int) (bits ^ (bits >> 32));
  }

  @Override
  public boolean equals(final Object object) {
    if (object == null || !(object instanceof Host)) {
      return false;
    }

    if (this == object) {
      return true;
    }

    Host host = (Host) object;

    return this.address.equals(host.address) && this.port.equals(host.port);
  }

  @Override
  public String toString() {
    return String.format("%s:%s", this.address, this.port);
  }
}
