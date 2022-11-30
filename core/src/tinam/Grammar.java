package tinam;

import java.util.List;

public sealed interface Grammar {
  record Unconditional(String scope) implements Grammar {}
  record Simple(String scope, Rule rule) implements Grammar {}
  record Delimitated(String scope, Rule begin, Rule end) implements Grammar {}
  record Combined(List<Grammar> combined) implements Grammar {}

  static Grammar unconditional(String scope) {
    return new Unconditional(scope);
  }
  static Grammar simple(String scope, Rule rule) {
    return new Simple(scope, rule);
  }
  static Grammar delimitated(String scope, Rule begin, Rule end) {
    return new Delimitated(scope, begin, end);
  }
  static Grammar combined(Grammar... combined) {
    return new Combined(List.of(combined));
  }
}
