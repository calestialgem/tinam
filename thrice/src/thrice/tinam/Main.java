package thrice.tinam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class Main {
  public static void main(String[] arguments) {
    var output = Path.of("thrice.tmLanguage.json");
    try {
      Files.writeString(output, Generator.generate());
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
