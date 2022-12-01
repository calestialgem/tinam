package tinam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record Grammar(String name, String scopeName, List<Rule> topLevel) {
  public static Grammar combined(String name, String scopeName,
    Rule... combined) {
    return combined(name, scopeName, List.of(combined));
  }
  public static Grammar combined(String name, String scopeName,
    Collection<Rule> set) {
    var list = new ArrayList<Rule>();
    for (var member : set) {
      if (member instanceof Rule.Combined nested && nested.name().isEmpty()) {
        list.addAll(nested.set());
      } else {
        list.add(member);
      }
    }
    return new Grammar(name, scopeName, Collections.unmodifiableList(list));
  }

  public void textmate(StringBuilder builder) {
    builder.append('{');
    appendMapping(builder, "name", name);
    builder.append(',');
    appendMapping(builder, "scopeName", scopeName);
    builder.append(",\"patterns\":[");
    topLevel.get(0).access(builder);
    for (var i = 1; i < topLevel.size(); i++) {
      builder.append(',');
      topLevel.get(i).access(builder);
    }
    builder.append("],\"repository\":{");
    topLevel.get(0).define(builder);
    for (var i = 1; i < topLevel.size(); i++) {
      builder.append(',');
      topLevel.get(i).define(builder);
    }
    builder.append("}}");
  }

  public String textmate() {
    var builder = new StringBuilder();
    textmate(builder);
    return builder.toString();
  }

  private static void appendMapping(StringBuilder builder, String key,
    String value) {
    appendString(builder, key);
    builder.append(':');
    appendString(builder, value);
  }

  private static void appendString(StringBuilder builder, String appended) {
    builder.append('"');
    builder.append(appended);
    builder.append('"');
  }
}
