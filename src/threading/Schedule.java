package threading;

// General
import java.util.Timer;
import java.util.TimerTask;

// I/O
import java.io.IOException;

public final class Schedule {
  public Schedule(final Task task, final int delay, final int interval) {
    if (task == null) {
      throw new IllegalArgumentException("A task must be specified");
    }

    if (delay < 0) {
      throw new IllegalArgumentException("Delay cannot be negative");
    }

    if (interval < 0) {
      throw new IllegalArgumentException("Interval cannot be negative");
    }

    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      public void run() {
        try {
          task.run();
        } catch (IOException ex) {
          this.cancel();
        }
      }
    }, delay, interval);
  }

  public Schedule(final Task task, final int interval) {
    this(task, 0, interval);
  }
}
