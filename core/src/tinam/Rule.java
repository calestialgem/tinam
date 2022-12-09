package tinam;

import java.util.List;
import java.util.Optional;

public sealed interface Rule {
  record Data(Optional<String> scope, List<Rule> inner) {}

  record Unconditional(Data data) implements Rule {}
  record Conditional(Data data, Pattern condition) implements Rule {}
  record Delimitated(Data data, Pattern initializer, Pattern terminator)
    implements Rule {}

  static Data scoped(String scope) {
    return new Data(Optional.of(scope), List.of());
  }
  static Data combined(List<Rule> inner) {
    return new Data(Optional.empty(), inner);
  }
  static Data data(String scope, List<Rule> inner) {
    return new Data(Optional.of(scope), inner);
  }

  static Unconditional unconditional(Data data) {
    return new Unconditional(data);
  }
  static Conditional conditional(Data data, Pattern condition) {
    return new Conditional(data, condition);
  }
  static Delimitated delimitated(Data data, Pattern initializer,
    Pattern terminator) {
    return new Delimitated(data, initializer, terminator);
  }

  Data data();
}
