package console;

public final class Arguments {
  private final String[] arguments;
  private volatile int next = 0;

  public Arguments(final String input) {
    if (input == null) {
      throw new IllegalArgumentException("Input must be specified");
    }

    this.arguments = input.split("\\s+");
  }

  public Arguments(final String... input) {
    if (input == null || input.length == 0) {
      throw new IllegalArgumentException("Input must be specified");
    }

    this.arguments = input;
  }

  public int length() {
    return arguments.length;
  }

  public synchronized String read() {
    return arguments.length > next ? arguments[next++] : null;
  }

  public synchronized short readShort() {
    return Short.parseShort(this.read());
  }

  public synchronized int readInt() {
    return Integer.parseInt(this.read());
  }

  public synchronized long readLong() {
    return Long.parseLong(this.read());
  }

  public synchronized float readFloat() {
    return Float.parseFloat(this.read());
  }

  public synchronized double readDouble() {
    return Double.parseDouble(this.read());
  }
}
