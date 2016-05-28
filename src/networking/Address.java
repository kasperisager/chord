package networking;

// I/O
import java.io.Serializable;

/**
 * The immutable {@link Address} class describes a DNS or an IP address.
 */
public final class Address implements Serializable {
  /**
   * Unique ID used for serialization.
   */
  private static final long serialVersionUID = 83610571;

  /**
   * The DNS or IP address.
   */
  private final String address;

  /**
   * Initialize a new {@link Address}.
   *
   * @param address The string representation of the {@link Address}. Can be both a DNS name as well as an IP address.
   */
  public Address(final String address) {
    if (address == null || address.isEmpty()) {
      throw new IllegalArgumentException("A host must be specified");
    }

    this.address = address;
  }

  /**
   * Get the value of the {@link Address}.
   *
   * @return The value of the {@link Address}.
   */
  public String value() {
    return this.address;
  }

  @Override
  public int hashCode() {
    return this.address.hashCode();
  }

  @Override
  public boolean equals(final Object object) {
    if (object == null || !(object instanceof Address)) {
      return false;
    }

    if (this == object) {
      return true;
    }

    Address address = (Address) object;

    return this.address.equals(address.address);
  }

  @Override
  public String toString() {
    return this.address;
  }
}
