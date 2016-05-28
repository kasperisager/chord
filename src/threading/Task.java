package threading;

// I/O
import java.io.IOException;

public interface Task {
  void run() throws IOException;
}
