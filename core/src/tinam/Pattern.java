package tinam;

import java.util.List;
import java.util.Optional;

public sealed interface Pattern {
  record One(String set) implements Pattern {}
  record Not(String set) implements Pattern {}
  record Any() implements Pattern {}
  record All(String characters) implements Pattern {}
  record Range(char first, char last) implements Pattern {}
  record Start() implements Pattern {}
  record End() implements Pattern {}
  record Or(List<Pattern> alternatives) implements Pattern {}
  record And(List<Pattern> sequence) implements Pattern {}
  record Repeat(Pattern repeated, Optional<Integer> minimum,
    Optional<Integer> maximum) implements Pattern {}
  record Unmatched(Pattern unmatched) implements Pattern {}

  static Pattern one(String set) { return new One(set); }
  static Pattern not(String set) { return new Not(set); }
  static Pattern any() { return new Any(); }
  static Pattern all(String characters) { return new All(characters); }
  static Pattern range(char first, char last) { return new Range(first, last); }
  static Pattern start() { return new Start(); }
  static Pattern end() { return new End(); }
  static Pattern or(Pattern... alternatives) {
    return new Or(List.of(alternatives));
  }
  static Pattern and(Pattern... sequence) { return new And(List.of(sequence)); }
  static Pattern optional(Pattern pattern) {
    return new Repeat(pattern, Optional.empty(), Optional.of(1));
  }
  static Pattern zeroOrMore(Pattern repeated) {
    return new Repeat(repeated, Optional.empty(), Optional.empty());
  }
  static Pattern oneOrMore(Pattern repeated) {
    return givenOrMore(repeated, 1);
  }
  static Pattern givenOrMore(Pattern repeated, Integer minimum) {
    return new Repeat(repeated, Optional.of(minimum), Optional.empty());
  }
  static Pattern givenOrLess(Pattern repeated, Integer maximum) {
    return new Repeat(repeated, Optional.empty(), Optional.of(maximum));
  }
  static Pattern fixedTimes(Pattern repeated, Integer times) {
    return repeat(repeated, times, times);
  }
  static Pattern repeat(Pattern repeated, Integer minimum, Integer maximum) {
    return new Repeat(repeated, Optional.of(minimum), Optional.of(maximum));
  }
  static Pattern unmatched(Pattern unmatched) {
    return new Unmatched(unmatched);
  }
}
