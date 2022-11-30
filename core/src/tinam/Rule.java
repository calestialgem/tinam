package tinam;

import java.util.Optional;

public record Rule(Pattern pattern, Optional<Grammar> grammar) {
  public static Rule base(Pattern pattern) {
    return new Rule(pattern, Optional.empty());
  }
  public static Rule recursive(Pattern pattern, Grammar grammar) {
    return new Rule(pattern, Optional.of(grammar));
  }
}
