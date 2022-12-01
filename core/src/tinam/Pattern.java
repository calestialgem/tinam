package tinam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public sealed interface Pattern {
  record One(String set) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      if (set.length() == 1) {
        appendEscaped(builder, set);
        return;
      }
      builder.append('[');
      appendEscapedSet(builder, set);
      builder.append(']');
    }
    @Override public void groups(List<String> list) {}
    @Override public Pattern not() { return notOne(set); }
  }

  record NotOne(String set) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("[^");
      appendEscapedSet(builder, set);
      builder.append(']');
    }
    @Override public void groups(List<String> list) {}
    @Override public Pattern not() { return one(set); }
  }

  record Any() implements Pattern {
    @Override public void regex(StringBuilder builder) { builder.append("."); }
    @Override public void groups(List<String> list) {}
    @Override public Pattern not() {
      throw new RuntimeException("Cannot negate any pattern!");
    }
  }

  record All(String characters) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      appendEscaped(builder, characters);
    }
    @Override public void groups(List<String> list) {}
    @Override public Pattern not() {
      throw new RuntimeException("Cannot negate all pattern!");
    }
  }

  record Range(char first, char last) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append('[');
      appendEscapedRange(builder, first, last);
      builder.append(']');
    }
    @Override public void groups(List<String> list) {}
    @Override public Pattern not() { return notRange(first, last); }
  }

  record NotRange(char first, char last) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("[^");
      appendEscapedRange(builder, first, last);
      builder.append(']');
    }
    @Override public void groups(List<String> list) {}
    @Override public Pattern not() { return range(first, last); }
  }

  record Start() implements Pattern {
    @Override public void regex(StringBuilder builder) { builder.append("^"); }
    @Override public void groups(List<String> list) {}
    @Override public Pattern not() {
      throw new RuntimeException("Cannot negate start pattern!");
    }
  }

  record End() implements Pattern {
    @Override public void regex(StringBuilder builder) { builder.append("$"); }
    @Override public void groups(List<String> list) {}
    @Override public Pattern not() {
      throw new RuntimeException("Cannot negate end pattern!");
    }
  }

  record Or(List<Pattern> alternatives) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      if (isJoinedSets()) {
        joinedSetsRegex(builder);
        return;
      }
      if (isJoinedNotSets()) {
        joinedNotSetsRegex(builder);
        return;
      }
      alternatives.get(0).unitRegex(builder);
      for (var i = 1; i < alternatives.size(); i++) {
        builder.append('|');
        alternatives.get(i).unitRegex(builder);
      }
    }
    @Override public void unitRegex(StringBuilder builder) {
      appendAsUnit(builder, this);
    }
    @Override public void groups(List<String> list) {
      for (var alternative : alternatives) alternative.groups(list);
    }
    @Override public Pattern not() {
      return Pattern.and(alternatives.stream().map(Pattern::not).toList());
    }

    private boolean isJoinedSets() {
      for (var alternative : alternatives) {
        switch (alternative) {
        case One one:
          break;
        case Range range:
          break;
        default:
          return false;
        }
      }
      return true;
    }
    private void joinedSetsRegex(StringBuilder builder) {
      builder.append('[');
      for (var alternative : alternatives) {
        switch (alternative) {
        case One one:
          appendEscapedSet(builder, one.set);
          break;
        case Range range:
          appendEscapedRange(builder, range.first, range.last);
          break;
        default:
          throw new RuntimeException(
            "Unexpected pattern type %s!".formatted(alternative));
        }
      }
      builder.append(']');
    }

    private boolean isJoinedNotSets() {
      for (var alternative : alternatives) {
        switch (alternative) {
        case NotOne one:
          break;
        case NotRange range:
          break;
        default:
          return false;
        }
      }
      return true;
    }
    private void joinedNotSetsRegex(StringBuilder builder) {
      builder.append("[^");
      for (var alternative : alternatives) {
        switch (alternative) {
        case NotOne one:
          appendEscapedSet(builder, one.set);
          break;
        case NotRange range:
          appendEscapedRange(builder, range.first, range.last);
          break;
        default:
          throw new RuntimeException(
            "Unexpected pattern type %s!".formatted(alternative));
        }
      }
      builder.append(']');
    }
  }

  record And(List<Pattern> sequence) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      for (var sequent : sequence) { sequent.unitRegex(builder); }
    }
    @Override public void unitRegex(StringBuilder builder) {
      appendAsUnit(builder, this);
    }
    @Override public void groups(List<String> list) {
      for (var sequent : sequence) sequent.groups(list);
    }
    @Override public Pattern not() {
      return Pattern.or(sequence.stream().map(Pattern::not).toList());
    }
  }

  record BoundedRepeat(Pattern repeated, int minimum, int maximum)
    implements Pattern {
    @Override public void regex(StringBuilder builder) {
      repeated.unitRegex(builder);
      if (minimum == 0 && maximum == 1) {
        builder.append('?');
        return;
      }
      builder.append('{');
      builder.append(minimum);
      builder.append(',');
      builder.append(maximum);
      builder.append('}');
    }
    @Override public void groups(List<String> list) { repeated.groups(list); }
    @Override public Pattern not() {
      throw new RuntimeException("Cannot negate bounded repeat pattern!");
    }
  }

  record InfiniteRepeat(Pattern repeated, int minimum) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      repeated.unitRegex(builder);
      switch (minimum) {
      case 0:
        builder.append('*');
        break;
      case 1:
        builder.append('+');
        break;
      default:
        builder.append('{');
        builder.append(minimum);
        builder.append(',');
        builder.append('}');
      }
    }
    @Override public void groups(List<String> list) { repeated.groups(list); }
    @Override public Pattern not() {
      throw new RuntimeException("Cannot negate infinite repeat pattern!");
    }
  }

  record Lookup(Pattern pattern, boolean wanted, boolean behind)
    implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append("(?");
      if (behind) { builder.append('<'); }
      builder.append(wanted ? '=' : '!');
      pattern.regex(builder);
      builder.append(')');
    }
    @Override public void groups(List<String> list) { pattern.groups(list); }
    @Override public Pattern not() {
      return new Lookup(pattern, !wanted, behind);
    }
  }

  record Group(Pattern pattern, String name) implements Pattern {
    @Override public void regex(StringBuilder builder) {
      builder.append('(');
      pattern.regex(builder);
      builder.append(')');
    }
    @Override public void groups(List<String> list) {
      list.add(name);
      pattern.groups(list);
    }
    @Override public Pattern not() {
      throw new RuntimeException("Cannot negate group pattern!");
    }
  }

  static Pattern one(String set) {
    validateSet(set);
    return new One(set);
  }

  static Pattern notOne(String set) {
    validateSet(set);
    return new NotOne(set);
  }

  static Pattern any() { return new Any(); }

  static Pattern all(String characters) { return new All(characters); }

  static Pattern range(char first, char last) { return new Range(first, last); }

  static Pattern notRange(char first, char last) {
    return new NotRange(first, last);
  }

  static Pattern start() { return new Start(); }

  static Pattern end() { return new End(); }

  static Pattern or(Pattern... alternatives) {
    return or(List.of(alternatives));
  }
  static Pattern or(Collection<Pattern> alternatives) {
    if (alternatives.size() < 2)
      throw new RuntimeException("There must be at least 2 alternatives!");
    var list = new ArrayList<Pattern>();
    for (var alternative : alternatives) {
      if (alternative instanceof Or or) {
        list.addAll(or.alternatives);
      } else {
        list.add(alternative);
      }
    }
    return new Or(list);
  }

  static Pattern and(Pattern... sequence) { return and(List.of(sequence)); }
  static Pattern and(Collection<Pattern> sequence) {
    if (sequence.size() < 2)
      throw new RuntimeException("There must be at least 2 in sequence!");
    var list = new ArrayList<Pattern>();
    for (var sequent : sequence) {
      if (sequent instanceof And and) {
        list.addAll(and.sequence);
      } else {
        list.add(sequent);
      }
    }
    return new And(list);
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

  static Pattern after(Pattern pattern) {
    return new Lookup(pattern, true, true);
  }
  static Pattern notAfter(Pattern pattern) {
    return new Lookup(pattern, false, true);
  }
  static Pattern before(Pattern pattern) {
    return new Lookup(pattern, true, false);
  }
  static Pattern notBefore(Pattern pattern) {
    return new Lookup(pattern, false, false);
  }

  static Pattern group(Pattern pattern, String name) {
    return new Group(pattern, name);
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

  static void appendEscapedRange(StringBuilder builder, char first, char last) {
    appendEscapedMember(builder, first);
    builder.append('-');
    appendEscapedMember(builder, last);
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

  static void appendAsUnit(StringBuilder builder, Pattern pattern) {
    builder.append("(?:");
    pattern.regex(builder);
    builder.append(')');
  }

  Pattern not();

  void groups(List<String> list);

  default List<String> groups() {
    var list = new ArrayList<String>();
    groups(list);
    return Collections.unmodifiableList(list);
  }

  void regex(StringBuilder builder);

  default void unitRegex(StringBuilder builder) { regex(builder); }

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
