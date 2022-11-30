package tinam;

import java.util.Collection;

public record Grammar(String name, String scopeName, Rule topLevel) {
  public static Grammar combined(String name, String scopeName,
    Rule... combined) {
    return new Grammar(name, scopeName, Rule.combined(combined));
  }
  public static Grammar combined(String name, String scopeName,
    Collection<Rule> combined) {
    return new Grammar(name, scopeName, Rule.combined(combined));
  }

  public void textmate(StringBuilder builder) {
    builder.append('{');
    appendMapping(builder, "name", name);
    builder.append(',');
    appendMapping(builder, "scopeName", scopeName);
    builder.append(',');
    builder
      .append("\"patterns\":[{\"include\":\"#g\"}],\"repository\":{\"g\":{");
    topLevel.ruleList(builder);
    builder.append("}}}");
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
