package tinam;

public enum NameProvider {
  INSTANCE;

  private static final String NAME_DIGITS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

  private int current = 0;

  public String get() {
    var builder   = new StringBuilder();
    int remaining = current++;
    do {
      var index = remaining % NAME_DIGITS.length();
      builder.append(NAME_DIGITS.charAt(index));
      remaining -= index;
      remaining /= NAME_DIGITS.length();
    } while (remaining != 0);
    return builder.reverse().toString();
  }
}
