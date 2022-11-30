package tinam;

import java.util.List;

public sealed interface Pattern {
  record One(String set) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:[");
      appendEscapedSet(builder, set);
      builder.append("])");
    }
  }

  record Not(String set) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:[");
      builder.append('^');
      appendEscapedSet(builder, set);
      builder.append("])");
    }
  }

  record Any() implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:.)");
    }
  }

  record All(String characters) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:");
      appendEscaped(builder, characters);
      builder.append(')');
    }
  }

  record Range(char first, char last) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:[");
      appendEscapedMember(builder, first);
      builder.append('-');
      appendEscapedCharacter(builder, last);
      builder.append("])");
    }
  }

  record Start() implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:^)");
    }
  }

  record End() implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:$)");
    }
  }

  record Or(List<Pattern> alternatives) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:");
      alternatives.get(0).regex(builder);
      for (var i = 1; i < alternatives.size(); i++) {
        builder.append('|');
        alternatives.get(i).regex(builder);
      }
      builder.append(')');
    }
  }

  record And(List<Pattern> sequence) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:");
      for (var p : sequence) { p.regex(builder); }
      builder.append(')');
    }
  }

  record BoundedRepeat(Pattern repeated, int minimum, int maximum)
    implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:");
      repeated.regex(builder);
      builder.append('{');
      builder.append(minimum);
      builder.append(',');
      builder.append(maximum);
      builder.append('}');
      builder.append(')');
    }
  }

  record InfiniteRepeat(Pattern repeated, int minimum) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:");
      repeated.regex(builder);
      builder.append('{');
      builder.append(minimum);
      builder.append(',');
      builder.append('}');
      builder.append(')');
    }
  }

  record Word(Pattern pattern) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:");
      builder.append("\\b");
      pattern.regex(builder);
      builder.append("\\b");
      builder.append(')');
    }
  }

  record Unmatched(Pattern unmatched) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?:");
      builder.append("(?=");
      unmatched.regex(builder);
      builder.append(')');
      builder.append(')');
    }
  }

  static Pattern one(String set) {
    validateSet(set);
    return new One(set);
  }

  static Pattern not(String set) {
    validateSet(set);
    return new Not(set);
  }

  static Pattern any() { return new Any(); }

  static Pattern all(String characters) { return new All(characters); }

  static Pattern range(char first, char last) { return new Range(first, last); }

  static Pattern start() { return new Start(); }

  static Pattern end() { return new End(); }

  static Pattern or(Pattern... alternatives) {
    if (alternatives.length < 2)
      throw new RuntimeException("There must be at least 2 alternatives!");
    return new Or(List.of(alternatives));
  }

  static Pattern and(Pattern... sequence) {
    if (sequence.length < 2)
      throw new RuntimeException("There must be at least 2 in sequence!");
    return new And(List.of(sequence));
  }

  static Pattern zeroOrMore(Pattern repeated) {
    return new InfiniteRepeat(repeated, 0);
  }
  static Pattern oneOrMore(Pattern repeated) {
    return new InfiniteRepeat(repeated, 1);
  }
  static Pattern givenOrMore(Pattern repeated, int minimum) {
    return new InfiniteRepeat(repeated, minimum);
  }

  static Pattern optional(Pattern pattern) {
    return new BoundedRepeat(pattern, 0, 1);
  }
  static Pattern givenOrLess(Pattern repeated, int maximum) {
    if (maximum < 1) throw new RuntimeException(
      "Given or less upper bound must be at least 1!");
    return new BoundedRepeat(repeated, 0, maximum);
  }
  static Pattern fixedTimes(Pattern repeated, int times) {
    if (times < 2)
      throw new RuntimeException("Fixed repeat must be at least 2 times!");
    return new BoundedRepeat(repeated, times, times);
  }
  static Pattern repeat(Pattern repeated, int minimum, int maximum) {
    if (maximum < minimum) throw new RuntimeException(
      "Repeat upper bound is less than the lower bound!");
    if (minimum == maximum && minimum < 2)
      throw new RuntimeException("Fixed repeat must be at least 2 times!");
    if (maximum < 1)
      throw new RuntimeException("Repeat upper bound must be at least 1!");
    return new BoundedRepeat(repeated, minimum, maximum);
  }

  static Pattern word(Pattern pattern) { return new Word(pattern); }

  static Pattern unmatched(Pattern unmatched) {
    return new Unmatched(unmatched);
  }

  static void validateSet(String set) {
    if (set.length() == 0) throw new RuntimeException("Set is empty!");
    for (var i = 0; i < set.length(); i++) {
      var c         = set.charAt(i);
      int lastIndex = set.lastIndexOf(c);
      if (lastIndex != i) throw new RuntimeException(
        "Repeating '%c' character in set at indices [%d] and [%d]!".formatted(c,
          i, lastIndex));
    }
  }

  static void appendEscapedSet(StringBuilder builder, String set) {
    for (var member : set.toCharArray()) {
      appendEscapedMember(builder, member);
    }
  }

  static void appendEscapedMember(StringBuilder builder, char member) {
    switch (member) {
    case '\\', '^', '[', ']', '-':
      builder.append('\\');
      //$FALL-THROUGH$
    default:
      builder.append(member);
    }
  }

  static void appendEscaped(StringBuilder builder, String characters) {
    for (var character : characters.toCharArray()) {
      appendEscapedCharacter(builder, character);
    }
  }

  static void appendEscapedCharacter(StringBuilder builder, char character) {
    switch (character) {
    case '\\', '^', '$', '[', ']', '(', ')', '{', '}', '.', '+', '*', '?', '!':
      builder.append('\\');
      //$FALL-THROUGH$
    default:
      builder.append(character);
    }
  }

  void regex(StringBuilder builder);

  default String regex() {
    var builder = new StringBuilder();
    regex(builder);
    return builder.toString();
  }

  default String escapedRegex() {
    var builder = new StringBuilder();
    for (var character : regex().toCharArray()) {
      switch (character) {
      case '\\', '"':
        builder.append('\\');
        //$FALL-THROUGH$
      default:
        builder.append(character);
      }
    }
    return builder.toString();
  }
}
