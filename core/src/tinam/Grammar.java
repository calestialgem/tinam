package tinam;

import java.util.List;
import java.util.Map;

public record Grammar(String name, String extension, List<Rule> topLevel,
  Map<Rule, String> repository) {
  public static Grammar of(String name, String extension, List<Rule> topLevel,
    Map<Rule, String> repository) {
    return new Grammar(name, extension, topLevel, repository);
  }
}
