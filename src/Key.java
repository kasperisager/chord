import java.io.Serializable;

public final class Key implements Serializable {
  /**
   * Unique ID used for serialization.
   */
  private static final long serialVersionUID = 81263671;

  /**
   * The size of {@link Peer} keys in bits.
   */
  public static final byte SIZE = 32;

  public static final int MINIMUM = 0;

  public static final int MAXIMUM = (int) Math.pow(2, SIZE);

  private final long key;

  public Key(final long key) {
    if (key < MINIMUM) {
      throw new IllegalArgumentException("Key cannot be smaller than " + MINIMUM);
    }

    this.key = key % MAXIMUM;
  }

  public Key(final int key) {
    this(key & 0x00000000ffffffffL);
  }

  /**
   * Check if a {@link Key} lies between a lower (exclusive) and upper (inclusive)
   * {@link Key}.
   *
   * @param lower The lower bound.
   * @param upper The upper bound.
   * @return      A boolean indicating whether or not the number lies between
   *              the lower and upper bound.
   */
  public boolean isBetween(final Key lower, final Key upper) {
    if (lower.key < upper.key) {
      return this.key > lower.key && this.key <= upper.key;
    } else {
      return this.key > lower.key || this.key <= upper.key;
    }
  }

  public Key shift(final int bits) {
    return new Key(this.key + 1 << bits);
  }

  @Override
  public int hashCode() {
    return (int) this.key;
  }

  @Override
  public boolean equals(final Object object) {
    if (object == null || !(object instanceof Key)) {
      return false;
    }

    if (this == object) {
      return true;
    }

    Key key = (Key) object;

    return this.key == key.key;
  }

  @Override
  public String toString() {
    return this.key + "";
  }
}
