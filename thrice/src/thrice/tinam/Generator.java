package thrice.tinam;

import tinam.Pattern;
import tinam.Rule;

import static tinam.Pattern.*;

import tinam.Grammar;

public final class Generator {
  public static String generate() {
    return new Generator().grammar().textmate();
  }

  private final Pattern lowercase    = range('a', 'z');
  private final Pattern uppercase    = range('A', 'Z');
  private final Pattern letter       = or(lowercase, uppercase);
  private final Pattern decimal      = range('0', '9');
  private final Pattern alphanumeric = or(letter, decimal);

  private final Pattern fractionSeparator = all(".");
  private final Pattern digitSeparator    = all("'");
  private final Pattern sign              = one("+-");
  private final Pattern numberIndicator   = all("0");

  private final Pattern decimalExponentIndicator = one("eE");
  private final Pattern decimalNumber            =
    separate(numberBody(decimal, decimalExponentIndicator));

  private final Pattern hexadecimal                  =
    or(decimal, range('a', 'f'), range('A', 'F'));
  private final Pattern hexadecimalIndicator         = one("xX");
  private final Pattern hexadecimalExponentIndicator = one("pP");
  private final Pattern hexadecimalNumber            =
    separate(indicatedNumber(hexadecimalIndicator, hexadecimal,
      hexadecimalExponentIndicator));

  private final Pattern octal                  = range('0', '7');
  private final Pattern octalIndicator         = one("oO");
  private final Pattern octalExponentIndicator = one("pP");
  private final Pattern octalNumber            =
    separate(indicatedNumber(octalIndicator, octal, octalExponentIndicator));

  private final Pattern binary                  = range('0', '1');
  private final Pattern binaryIndicator         = one("bB");
  private final Pattern binaryExponentIndicator = one("pP");
  private final Pattern binaryNumber            =
    separate(indicatedNumber(binaryIndicator, binary, binaryExponentIndicator));

  private final Pattern lineCommentIndicator = all("#");
  private final Pattern blockCommentBegin    = all("#{");
  private final Pattern blockCommentEnd      = all("}#");

  private final Rule decimalNumberRule     =
    numberRule(decimalNumber, "decimal");
  private final Rule hexadecimalNumberRule =
    numberRule(hexadecimalNumber, "hexadecimal");
  private final Rule octalNumberRule       = numberRule(octalNumber, "octal");
  private final Rule binaryNumberRule      = numberRule(binaryNumber, "binary");
  private final Rule numberRule            = Rule.combined(decimalNumberRule,
    hexadecimalNumberRule, octalNumberRule, binaryNumberRule);

  private final Rule blockCommentBeginPunctuationRule =
    Rule.scope(Rule.conditional(blockCommentBegin),
      "punctuation.definition.comment.begin");
  private final Rule blockCommentEndPunctuationRule   = Rule.scope(
    Rule.conditional(blockCommentEnd), "punctuation.definition.comment.end");
  private final Rule blockCommentRule                 = Rule.scope(
    Rule.name(Rule.delimitated(captureSimple(blockCommentBeginPunctuationRule),
      captureSimple(blockCommentEndPunctuationRule)), "comment_block"),
    "comment.block.documentation");

  private final Rule lineCommentIndicatorPunctuationRule =
    Rule.scope(Rule.conditional(lineCommentIndicator),
      "punctuation.definition.comment.indicator");
  private final Rule lineCommentRule                     = Rule.scope(Rule.name(
    Rule.delimitated(captureSimple(lineCommentIndicatorPunctuationRule), end()),
    "comment_line"), "comment.line.number-sign");

  private final Rule commentRule =
    Rule.combined(blockCommentRule, lineCommentRule);

  private Generator() {}

  private Grammar grammar() {
    return Grammar.combined("Thrice", "tr", numberRule, commentRule);
  }

  private Rule numberRule(Pattern pattern, String name) {
    return Rule.scope(Rule.name(Rule.conditional(pattern), "numeric_" + name),
      "constant.numeric." + name);
  }

  private Pattern separate(Pattern separated) {
    return and(notAfter(alphanumeric), separated, notBefore(alphanumeric));
  }

  private Pattern indicatedNumber(Pattern indicator, Pattern digit,
    Pattern exponentIndicator) {
    return and(numberIndicator, indicator,
      numberBody(digit, exponentIndicator));
  }

  private Pattern numberBody(Pattern digit, Pattern exponentIndicator) {
    return and(plainNumber(digit),
      optional(and(fractionSeparator, plainNumber(digit))),
      optional(and(exponentIndicator, optional(sign), plainNumber(decimal))));
  }

  private Pattern plainNumber(Pattern digit) {
    return and(digit, zeroOrMore(and(optional(digitSeparator), digit)));
  }
}
