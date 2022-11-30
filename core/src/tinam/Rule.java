package tinam;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public sealed interface Rule {
  record Unconditional(String scope) implements Rule {
    @Override public void textmate(StringBuilder builder) {
      builder.append('{');
      appendScope(builder, scope);
      builder.append('}');
    }
  }

  record Match(String scope, Pattern pattern) implements Rule {
    @Override public void textmate(StringBuilder builder) {
      builder.append('{');
      appendScope(builder, scope);
      builder.append(',');
      appendPattern(builder, "match", pattern);
      builder.append('}');
    }
  }

  record Delimitated(String scope, Pattern begin, Pattern end,
    Optional<Rule> inner) implements Rule {
    @Override public void textmate(StringBuilder builder) {
      builder.append('{');
      appendScope(builder, scope);
      appendPattern(builder, "begin", begin);
      builder.append(',');
      appendPattern(builder, "end", end);
      builder.append(',');
      if (inner.isPresent()) { inner.get().patternsTextmate(builder); }
      builder.append('}');
    }
  }

  record Combined(List<Rule> combined) implements Rule {
    @Override public void textmate(StringBuilder builder) {
      builder.append('{');
      patternsTextmate(builder);
      builder.append('}');
    }
    @Override public void patternsTextmate(StringBuilder builder) {
      builder.append("\"patterns\":[");
      combined.get(0).textmate(builder);
      for (var i = 1; i < combined.size(); i++) {
        builder.append(',');
        combined.get(i).textmate(builder);
      }
      builder.append("]");
    }
  }

  static Rule unconditional(String scope) { return new Unconditional(scope); }
  static Rule match(String scope, Pattern pattern) {
    return new Match(scope, pattern);
  }
  static Rule delimitated(String scope, Pattern begin, Pattern end) {
    return new Delimitated(scope, begin, end, Optional.empty());
  }
  static Rule higherDelimitated(String scope, Pattern begin, Pattern end,
    Rule inner) {
    return new Delimitated(scope, begin, end, Optional.of(inner));
  }
  static Rule combined(Rule... combined) {
    if (combined.length < 2)
      throw new RuntimeException("Must combine at least 2 rules!");
    return new Combined(List.of(combined));
  }
  static Rule combined(Collection<Rule> combined) {
    if (combined.size() < 2)
      throw new RuntimeException("Must combine at least 2 rules!");
    return new Combined(List.copyOf(combined));
  }

  static void appendScope(StringBuilder builder, String scope) {
    appendMapping(builder, "name", scope);
  }

  static void appendPattern(StringBuilder builder, String key, Pattern value) {
    appendMapping(builder, key, value.escapedRegex());
  }

  static void appendMapping(StringBuilder builder, String key, String value) {
    appendString(builder, key);
    builder.append(':');
    appendString(builder, value);
  }

  static void appendString(StringBuilder builder, String appended) {
    builder.append('"');
    builder.append(appended);
    builder.append('"');
  }

  void textmate(StringBuilder builder);

  default void patternsTextmate(StringBuilder builder) {
    builder.append("\"patterns\":[");
    textmate(builder);
    builder.append("]");
  }

  default String textmate() {
    var builder = new StringBuilder();
    textmate(builder);
    return builder.toString();
  }
}
