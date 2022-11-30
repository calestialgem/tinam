package tinam;

public class Main {
  public static void main(String[] args) {
    var decimalDigits     = Pattern.range('0', '9');
    var hexadecimalDigits = Pattern.or(Pattern.range('0', '9'),
      Pattern.range('a', 'f'), Pattern.range('A', 'F'));
    var decimal           = numberLiteral(decimalDigits, Pattern.one("eE"));
    var hexadecimal       =
      numberLiteral(Pattern.and(Pattern.one("0"), Pattern.one("xX")),
        hexadecimalDigits, Pattern.one("pP"));
    System.out.printf("Decimal: %s%nHexadecimal: %s%n", decimal.regex(),
      hexadecimal.regex());
  }

  private static Pattern numberLiteral(Pattern suffix, Pattern digit,
    Pattern exponentLetter) {
    return Pattern.word(Pattern.and(suffix, number(digit, exponentLetter)));
  }

  private static Pattern numberLiteral(Pattern digit, Pattern exponentLetter) {
    return Pattern.word(number(digit, exponentLetter));
  }

  private static Pattern number(Pattern digit, Pattern exponentLetter) {
    var     fractionSeparator = Pattern.one(".");
    Pattern wholeNumber       = plainNumber(digit);
    return Pattern.and(wholeNumber,
      Pattern.optional(Pattern.and(fractionSeparator, wholeNumber)),
      Pattern.optional(
        Pattern.and(exponentLetter, Pattern.optional(Pattern.one("+-")),
          plainNumber(Pattern.range('0', '9')))));
  }

  private static Pattern plainNumber(Pattern digit) {
    var deliminator = Pattern.one("'");
    return Pattern.and(digit,
      Pattern.zeroOrMore(Pattern.or(Pattern.optional(deliminator), digit)));
  }
}
