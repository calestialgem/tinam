package thrice.tinam;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

final class Main {
  public static void main(String[] arguments) {
    var start = System.nanoTime();
    var file  = "thrice.tmLanguage.json";
    try (var output =
      new BufferedOutputStream(new FileOutputStream(Path.of(file).toFile()))) {
      Generator.generate(output);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    var elapsed = System.nanoTime() - start;
    System.out.printf("Created `%s` in %.3f s.%n", file, elapsed / 1e9);
  }
}
