package tinam;

import java.util.Map;

public class Main {
  private static final Map<String, Pattern> UNNAMED_KEYWORDS =
    Map.ofEntries(Map.entry("private", Pattern.word(Pattern.all("private"))),
      Map.entry("protected", Pattern.word(Pattern.all("protected"))),
      Map.entry("public", Pattern.word(Pattern.all("public"))),
      Map.entry("import", Pattern.word(Pattern.all("import"))),
      Map.entry("const", Pattern.word(Pattern.all("const"))),
      Map.entry("var", Pattern.word(Pattern.all("var"))),
      Map.entry("func", Pattern.word(Pattern.all("func"))),
      Map.entry("proc", Pattern.word(Pattern.all("proc"))),
      Map.entry("entrypoint", Pattern.word(Pattern.all("entrypoint"))),
      Map.entry("free", Pattern.word(Pattern.all("free"))),
      Map.entry("interface", Pattern.word(Pattern.all("interface"))),
      Map.entry("struct", Pattern.word(Pattern.all("struct"))),
      Map.entry("enum", Pattern.word(Pattern.all("enum"))),
      Map.entry("union", Pattern.word(Pattern.all("union"))),
      Map.entry("if", Pattern.word(Pattern.all("if"))),
      Map.entry("else", Pattern.word(Pattern.all("else"))),
      Map.entry("for", Pattern.word(Pattern.all("for"))),
      Map.entry("while", Pattern.word(Pattern.all("while"))),
      Map.entry("do", Pattern.word(Pattern.all("do"))),
      Map.entry("switch", Pattern.word(Pattern.all("switch"))),
      Map.entry("case", Pattern.word(Pattern.all("case"))),
      Map.entry("default", Pattern.word(Pattern.all("default"))),
      Map.entry("fallthrough", Pattern.word(Pattern.all("fallthrough"))),
      Map.entry("break", Pattern.word(Pattern.all("break"))),
      Map.entry("continue", Pattern.word(Pattern.all("continue"))),
      Map.entry("return", Pattern.word(Pattern.all("return"))),
      Map.entry("mutable", Pattern.word(Pattern.all("mutable"))),
      Map.entry("shared", Pattern.word(Pattern.all("shared"))),
      Map.entry("volatile", Pattern.word(Pattern.all("volatile"))),
      Map.entry("alignas", Pattern.word(Pattern.all("alignas"))),
      Map.entry("threadlocal", Pattern.word(Pattern.all("threadlocal"))));

  private static final Map<String, Pattern> KEYWORDS = Map.ofEntries(
    Map.entry("private",
      Pattern.group(Pattern.word(Pattern.all("private")), "private")),
    Map.entry("protected",
      Pattern.group(Pattern.word(Pattern.all("protected")), "protected")),
    Map.entry("public",
      Pattern.group(Pattern.word(Pattern.all("public")), "public")),
    Map.entry("import",
      Pattern.group(Pattern.word(Pattern.all("import")), "import")),
    Map.entry("const",
      Pattern.group(Pattern.word(Pattern.all("const")), "const")),
    Map.entry("var", Pattern.group(Pattern.word(Pattern.all("var")), "var")),
    Map.entry("func", Pattern.group(Pattern.word(Pattern.all("func")), "func")),
    Map.entry("proc", Pattern.group(Pattern.word(Pattern.all("proc")), "proc")),
    Map.entry("entrypoint",
      Pattern.group(Pattern.word(Pattern.all("entrypoint")), "entrypoint")),
    Map.entry("free", Pattern.group(Pattern.word(Pattern.all("free")), "free")),
    Map.entry("interface",
      Pattern.group(Pattern.word(Pattern.all("interface")), "interface")),
    Map.entry("struct",
      Pattern.group(Pattern.word(Pattern.all("struct")), "struct")),
    Map.entry("enum", Pattern.group(Pattern.word(Pattern.all("enum")), "enum")),
    Map.entry("union",
      Pattern.group(Pattern.word(Pattern.all("union")), "union")),
    Map.entry("if", Pattern.group(Pattern.word(Pattern.all("if")), "if")),
    Map.entry("else", Pattern.group(Pattern.word(Pattern.all("else")), "else")),
    Map.entry("for", Pattern.group(Pattern.word(Pattern.all("for")), "for")),
    Map.entry("while",
      Pattern.group(Pattern.word(Pattern.all("while")), "while")),
    Map.entry("do", Pattern.group(Pattern.word(Pattern.all("do")), "do")),
    Map.entry("switch",
      Pattern.group(Pattern.word(Pattern.all("switch")), "switch")),
    Map.entry("case", Pattern.group(Pattern.word(Pattern.all("case")), "case")),
    Map.entry("default",
      Pattern.group(Pattern.word(Pattern.all("default")), "default")),
    Map.entry("fallthrough",
      Pattern.group(Pattern.word(Pattern.all("fallthrough")), "fallthrough")),
    Map.entry("break",
      Pattern.group(Pattern.word(Pattern.all("break")), "break")),
    Map.entry("continue",
      Pattern.group(Pattern.word(Pattern.all("continue")), "continue")),
    Map.entry("return",
      Pattern.group(Pattern.word(Pattern.all("return")), "return")),
    Map.entry("mutable",
      Pattern.group(Pattern.word(Pattern.all("mutable")), "mutable")),
    Map.entry("shared",
      Pattern.group(Pattern.word(Pattern.all("shared")), "shared")),
    Map.entry("volatile",
      Pattern.group(Pattern.word(Pattern.all("volatile")), "volatile")),
    Map.entry("alignas",
      Pattern.group(Pattern.word(Pattern.all("alignas")), "alignas")),
    Map.entry("threadlocal",
      Pattern.group(Pattern.word(Pattern.all("threadlocal")), "threadlocal")));

  public static void main(String[] args) {
    System.out
      .println(Grammar
        .combined("Thrice", "source.tr",
          Rule.match("constant.numeric.decimal.thrice", decimalLiteral()), Rule
            .match("constant.numeric.hexadecimal.thrice", hexadecimalLiteral()))
        .textmate());
  }

  private static Pattern identifier() {
    return Pattern.or(
      Pattern.and(Pattern.or(Pattern.range('a', 'z'), Pattern.range('A', 'Z')),
        Pattern.zeroOrMore(Pattern.or(Pattern.range('a', 'z'),
          Pattern.range('A', 'Z'), Pattern.range('0', '9')))),
      Pattern.and(Pattern.or(UNNAMED_KEYWORDS.values()), Pattern.one("_")));
  }

  private static Pattern decimalLiteral() {
    return Pattern.group(
      numberLiteral(Pattern.range('0', '9'), Pattern.one("eE")),
      "decimal_literal");
  }

  private static Pattern hexadecimalLiteral() {
    return Pattern
      .group(
        numberLiteral(Pattern.and(Pattern.one("0"), Pattern.one("xX")),
          Pattern.or(Pattern.range('0', '9'), Pattern.range('a', 'f'),
            Pattern.range('A', 'F')),
          Pattern.one("pP")),
        "hexadecimal_literal");
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
      Pattern.zeroOrMore(Pattern.and(Pattern.optional(deliminator), digit)));
  }
}
