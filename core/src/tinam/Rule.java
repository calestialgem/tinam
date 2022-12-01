package tinam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public sealed interface Rule {
  record Simple(Optional<String> name, Optional<String> scope,
    Optional<Pattern> pattern, Optional<Rule> inner) implements Rule {
    @Override public void access(StringBuilder builder) {
      if (name.isPresent()) {
        appendInclude(builder, name.get());
      } else {
        inline(builder);
      }
    }
    @Override public void inline(StringBuilder builder) {
      builder.append('{');
      appendScope(builder, scope);
      appendPattern(builder, "match", pattern);
      if (inner.isPresent()) { inner.get().list(builder); }
      builder.append('}');
    }
    @Override public void define(StringBuilder builder) {
      appendString(builder, name.get());
      builder.append('=');
      inline(builder);
    }
  }

  record Delimitated(Optional<String> name, Optional<String> scope,
    Pattern begin, Pattern end, Optional<Rule> inner) implements Rule {
    @Override public void access(StringBuilder builder) {
      if (name.isPresent()) {
        appendInclude(builder, name.get());
      } else {
        inline(builder);
      }
    }
    @Override public void inline(StringBuilder builder) {
      builder.append('{');
      appendScope(builder, scope);
      appendPattern(builder, "begin", begin);
      appendPattern(builder, "end", end);
      if (inner.isPresent()) { inner.get().list(builder); }
      builder.append('}');
    }
    @Override public void define(StringBuilder builder) {
      appendString(builder, name.get());
      builder.append('=');
      inline(builder);
    }
  }

  record Combined(Optional<String> name, Optional<String> scope, List<Rule> set)
    implements Rule {
    @Override public void access(StringBuilder builder) {
      if (name.isPresent()) {
        appendInclude(builder, name.get());
      } else {
        inline(builder);
      }
    }
    @Override public void inline(StringBuilder builder) {
      builder.append('{');
      list(builder);
      builder.append('}');
    }
    @Override public void list(StringBuilder builder) {
      builder.append("\"patterns\":[");
      for (var member : set) {
        appendComma(builder);
        member.access(builder);
      }
      builder.append("]");
    }
    @Override public void define(StringBuilder builder) {
      appendString(builder, name.get());
      builder.append('=');
      inline(builder);
      for (var member : set) {
        builder.append(',');
        member.define(builder);
      }
    }
  }

  static Rule unconditional() {
    return new Simple(Optional.empty(), Optional.empty(), Optional.empty(),
      Optional.empty());
  }
  static Rule conditional(Pattern pattern) {
    return new Simple(Optional.empty(), Optional.empty(), Optional.of(pattern),
      Optional.empty());
  }

  static Rule delimitated(Pattern begin, Pattern end) {
    return new Delimitated(Optional.empty(), Optional.empty(), begin, end,
      Optional.empty());
  }
  static Rule combined(Rule... combined) { return combined(List.of(combined)); }
  static Rule combined(Collection<Rule> set) {
    if (set.size() < 2)
      throw new RuntimeException("Must combine at least 2 rules!");
    var list = new ArrayList<Rule>();
    for (var member : set) {
      if (member instanceof Combined nested && nested.name.isEmpty()) {
        list.addAll(nested.set);
      } else {
        list.add(member);
      }
    }
    return new Combined(Optional.empty(), Optional.empty(),
      Collections.unmodifiableList(list));
  }

  static Rule name(Rule rule, String name) {
    switch (rule) {
    case Simple simple:
      if (simple.name.isPresent())
        throw new RuntimeException("Rule already has a name!");
      return new Simple(Optional.of(name), simple.scope, simple.pattern,
        simple.inner);
    case Delimitated delimitated:
      if (delimitated.name.isPresent())
        throw new RuntimeException("Rule already has a name!");
      return new Delimitated(Optional.of(name), delimitated.scope,
        delimitated.begin, delimitated.end, delimitated.inner);
    case Combined combined:
      if (combined.name.isPresent())
        throw new RuntimeException("Rule already has a name!");
      return new Combined(Optional.of(name), combined.scope, combined.set);
    }
  }
  static Rule scope(Rule rule, String scope) {
    switch (rule) {
    case Simple simple:
      if (simple.scope.isPresent())
        throw new RuntimeException("Rule already has a scope!");
      return new Simple(simple.name, Optional.of(scope), simple.pattern,
        simple.inner);
    case Delimitated delimitated:
      if (delimitated.scope.isPresent())
        throw new RuntimeException("Rule already has a scope!");
      return new Delimitated(delimitated.name, Optional.of(scope),
        delimitated.begin, delimitated.end, delimitated.inner);
    case Combined combined:
      if (combined.scope.isPresent())
        throw new RuntimeException("Rule already has a scope!");
      return new Combined(combined.name, Optional.of(scope), combined.set);
    }
  }
  static Rule inner(Rule rule, Rule inner) {
    switch (rule) {
    case Simple simple:
      if (simple.inner.isPresent())
        throw new RuntimeException("Rule already has an inner rule!");
      return new Simple(simple.name, simple.scope, simple.pattern,
        Optional.of(inner));
    case Delimitated delimitated:
      if (delimitated.inner.isPresent())
        throw new RuntimeException("Rule already has an inner rule!");
      return new Delimitated(delimitated.name, delimitated.scope,
        delimitated.begin, delimitated.end, Optional.of(inner));
    default:
      throw new RuntimeException("Rule cannot have inner rule!");
    }
  }

  static void appendInclude(StringBuilder builder, String name) {
    appendMapping(builder, "include", "#" + name);
  }

  static void appendScope(StringBuilder builder, Optional<String> scope) {
    if (scope.isPresent()) { appendScope(builder, scope.get()); }
  }
  static void appendScope(StringBuilder builder, String scope) {
    appendMapping(builder, "name", scope);
  }

  static void appendPattern(StringBuilder builder, String key,
    Optional<Pattern> value) {
    if (value.isPresent()) { appendPattern(builder, key, value.get()); }
  }
  static void appendPattern(StringBuilder builder, String key, Pattern value) {
    appendMapping(builder, key, value.escapedRegex());
  }

  static void appendMapping(StringBuilder builder, String key,
    Optional<String> value) {
    if (value.isPresent()) { appendMapping(builder, key, value.get()); }
  }
  static void appendMapping(StringBuilder builder, String key, String value) {
    appendComma(builder);
    appendString(builder, key);
    builder.append(':');
    appendString(builder, value);
  }

  static void appendComma(StringBuilder builder) {
    if (!builder.isEmpty()) {
      var last = builder.charAt(builder.length() - 1);
      if (last != '{' || last != '[') { builder.append(','); }
    }
  }

  static void appendString(StringBuilder builder, String appended) {
    builder.append('"');
    builder.append(appended);
    builder.append('"');
  }

  void define(StringBuilder builder);
  void inline(StringBuilder builder);
  void access(StringBuilder builder);

  default void list(StringBuilder builder) {
    builder.append("\"patterns\":[");
    access(builder);
    builder.append("]");
  }
}
