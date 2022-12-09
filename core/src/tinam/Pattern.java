package tinam;

import java.util.List;

public sealed interface Pattern {
  record One(String set) implements Pattern {}
  record NotOne(String set) implements Pattern {}
  record Range(char first, char last) implements Pattern {}
  record NotRange(char first, char last) implements Pattern {}
  record Any() implements Pattern {}
  record All(String characters) implements Pattern {}
  record Start() implements Pattern {}
  record End() implements Pattern {}
  record Or(List<Pattern> alternatives) implements Pattern {}
  record And(List<Pattern> sequence) implements Pattern {}
  record Repeat(Pattern repeated, int minimum, int maximum)
    implements Pattern {}
  record InfiniteRepeat(Pattern repeated, int minimum) implements Pattern {}
  record Lookup(Pattern looked, boolean wanted, boolean behind)
    implements Pattern {}
  record Capture(Pattern pattern, Rule rule) implements Pattern {}

  Any   ANY   = new Any();
  Start START = new Start();
  End   END   = new End();

  static One one(String set) {
    validateSet(set);
    return new One(set);
  }
  static NotOne notOne(String set) {
    validateSet(set);
    return new NotOne(set);
  }
  static Range range(char first, char last) {
    validateRange(first, last);
    return new Range(first, last);
  }
  static NotRange notRange(char first, char last) {
    validateRange(first, last);
    return new NotRange(first, last);
  }
  static Any any() { return ANY; }
  static All all(String characters) { return new All(characters); }
  static Start start() { return START; }
  static End end() { return END; }
  static Or or(List<Pattern> alternatives) {
    if (alternatives.size() < 2)
      throw new RuntimeException("Alternative count [%d] must be at least 2!"
        .formatted(alternatives.size()));
    return new Or(alternatives);
  }
  static And and(List<Pattern> sequence) {
    if (sequence.size() < 2) throw new RuntimeException(
      "Sequence length [%d] must be at least 2!".formatted(sequence.size()));
    return new And(sequence);
  }
  static Repeat optional(Pattern pattern) { return new Repeat(pattern, 0, 1); }
  static Repeat givenOrLess(Pattern repeated, int maximum) {
    if (maximum <= 0) throw new RuntimeException(
      "Given or less repeat maximum [%d] must be positive!".formatted(maximum));
    return new Repeat(repeated, 0, maximum);
  }
  static Repeat fixedTimes(Pattern repeated, int count) {
    if (count < 2) throw new RuntimeException(
      "Fixed times repeat count [%d] must be at least 2!".formatted(count));
    return new Repeat(repeated, count, count);
  }
  static Repeat repeat(Pattern repeated, int minimum, int maximum) {
    if (minimum > maximum) throw new RuntimeException(
      "Repeat minimum [%d] cannot be bigger than the maximum [%d]!"
        .formatted(minimum, maximum));
    if (minimum < 0) throw new RuntimeException(
      "Repeat minimum [%d] cannot be negative!".formatted(minimum));
    if (maximum <= 0) throw new RuntimeException(
      "Repeat maximum [%d] must be positive!".formatted(maximum));
    return new Repeat(repeated, minimum, maximum);
  }
  static InfiniteRepeat zeroOrMore(Pattern repeated) {
    return new InfiniteRepeat(repeated, 0);
  }
  static InfiniteRepeat oneOrMore(Pattern repeated) {
    return new InfiniteRepeat(repeated, 1);
  }
  static InfiniteRepeat givenOrMore(Pattern repeated, int minimum) {
    if (minimum < 0) throw new RuntimeException(
      "Given or more repeat minimum [%d] cannot be negative!"
        .formatted(minimum));
    return new InfiniteRepeat(repeated, minimum);
  }
  static Lookup after(Pattern looked) { return new Lookup(looked, true, true); }
  static Lookup notAfter(Pattern looked) {
    return new Lookup(looked, false, true);
  }
  static Lookup before(Pattern looked) {
    return new Lookup(looked, true, false);
  }
  static Lookup notBefore(Pattern looked) {
    return new Lookup(looked, false, false);
  }
  static Capture capture(Rule.Conditional captured) {
    return capture(captured.condition(), Rule.unconditional(captured.data()));
  }
  static Capture capture(Pattern pattern, Rule rule) {
    return new Capture(pattern, rule);
  }

  static void validateSet(String set) {
    if (set.isEmpty()) throw new RuntimeException("Set is empty!");
    for (var i = 0; i < set.length(); i++) {
      var c         = set.charAt(i);
      int lastIndex = set.lastIndexOf(c);
      if (lastIndex != i) throw new RuntimeException(
        "Set has duplicate character '%c' at multiple indices [%d, %d]!"
          .formatted(c, i, lastIndex));
    }
  }
  static void validateRange(char first, char last) {
    if (first > last) throw new RuntimeException(
      "First character '%c' cannot come after the last one '%c' in the range!");
    if (first == last) throw new RuntimeException(
      "First character is the same as the last one in the range!");
  }
}
