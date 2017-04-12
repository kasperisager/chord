package threading;

// Logging
import logging.Loggable;

// I/O
import java.io.Closeable;
import java.io.IOException;

public abstract class Worker<T extends Closeable> implements Loggable, Closeable, Runnable {
  private final T closeable;
  private final Thread thread;
  private volatile boolean isClosed;
  private volatile boolean isStarted;

  public Worker(final T closeable) {
    if (closeable == null) {
      throw new IllegalArgumentException("A closeable must be specified");
    }

    this.closeable = closeable;
    this.thread = new Thread(this);

    // Set the name of the worker thread as the name of the implementing class.
    this.thread.setName(this.getClass().getName());
  }

  /**
   * Check if the {@link Worker} is closed.
   *
   * @return A boolean indicating whether or not the {@link Worker} is closed.
   */
  public final boolean isClosed() {
    return this.isClosed;
  }

  /**
   * Check if the {@link Worker} is started.
   *
   * @return A boolean indicating whether or not the {@link Worker} is started.
   */
  public final boolean isStarted() {
    return this.isStarted;
  }

  /**
   * Start the {@link Worker}.
   *
   * <p>
   * This method is idempotent and calling it more than once will therefore have
   * no effect.
   */
  public final synchronized void start() {
    if (this.isClosed() || this.isStarted()) {
      return;
    }

    this.isStarted = true;
    this.thread.start();
  }

  /**
   * Close the {@link Worker} and its associated closeable and working thread.
   *
   * <p>
   * This method is idempotent and calling it more than once will therefore have
   * no effect.
   *
   * @throws IOException In case of an I/O error.
   */
  public final synchronized void close() throws IOException {
    if (this.isClosed()) {
      return;
    }

    this.isClosed = true;
    this.closeable.close();

    try {
      // Attempt joining in the worker thread, blocking until the thread has died.
      this.thread.join();
    } catch (InterruptedException ex) {
      this.log().severe(ex.getMessage());

      // Re-throw the checked exception as an unchecked runtime exception. This
      // is an extremely rare edge-case either way, so its preferred to handle
      // it as such.
      throw new RuntimeException(ex);
    }
  }

  /**
   * Run the {@link Worker} and its associated closeable.
   */
  public final void run() {
    try {
      this.run(this.closeable);
    } catch (IOException ex) {
      // Log the error and return. The method is run in a separate thread, and
      // we don't attempt to propagate the error to the calling thread.
      this.log().severe(ex.toString());
    }
  }

  protected abstract void run(final T closeable) throws IOException;
}
