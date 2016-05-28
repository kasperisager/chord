package console;

import java.util.Scanner;

import java.io.InputStream;
import java.io.PrintStream;

public final class Terminal {
  private final InputStream input;
  private final PrintStream print;

  public Terminal(final InputStream input, final PrintStream print) {
    if (input == null) {
      throw new IllegalArgumentException("Input stream must be specified");
    }

    if (print == null) {
      throw new IllegalArgumentException("Print stream must be specified");
    }

    this.input = input;
    this.print = print;
  }

  public void listen(final Parser parser) {
    Scanner scanner = new Scanner(this.input);
    this.caret();

    while (scanner.hasNextLine()) {
      Arguments arguments = new Arguments(scanner.nextLine());

      try {
        parser.parse(arguments);
      } catch (Exception ex) {
        this.println(ex.getMessage());
      }

      this.caret();
    }
  }

  public void print(final Object object) {
    this.print.print(object);
  }

  public void println(final Object object) {
    this.print.println(object);
  }

  private void caret() {
    this.print("\n❯ ");
  }
}
