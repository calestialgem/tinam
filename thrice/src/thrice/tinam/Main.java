package thrice.tinam;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

final class Main {
  public static void main(String[] arguments) {
    try (var output = new BufferedOutputStream(
      new FileOutputStream(Path.of("thrice.tmLanguage.json").toFile()))) {
      Generator.generate(output);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
