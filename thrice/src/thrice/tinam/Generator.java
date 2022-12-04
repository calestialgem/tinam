package thrice.tinam;

import tinam.Pattern;
import tinam.Rule;

import static tinam.Rule.*;
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

  private final Pattern documentationCodeDeliminator = all("`");
  private final Pattern documentationReferenceBegin  = all("[");
  private final Pattern documentationReferenceEnd    = all("]");
  private final Pattern documentationLinkBegin       = all("<");
  private final Pattern documentationLinkEnd         = all(">");
  private final Pattern lineCommentIndicator         = all("#");
  private final Pattern blockCommentBegin            = all("#{");
  private final Pattern blockCommentEnd              = all("}#");

  private final Rule decimalNumberRule     =
    numberRule(decimalNumber, "decimal");
  private final Rule hexadecimalNumberRule =
    numberRule(hexadecimalNumber, "hexadecimal");
  private final Rule octalNumberRule       = numberRule(octalNumber, "octal");
  private final Rule binaryNumberRule      = numberRule(binaryNumber, "binary");
  private final Rule numberRule            = combined(decimalNumberRule,
    hexadecimalNumberRule, octalNumberRule, binaryNumberRule);

  private final Rule documentationCodeRule      = documentationRule(
    documentationCodeDeliminator, documentationCodeDeliminator, "code");
  private final Rule documentationReferenceRule = documentationRule(
    documentationReferenceBegin, documentationReferenceEnd, "reference");
  private final Rule documentationLinkRule      =
    documentationRule(documentationLinkBegin, documentationLinkEnd, "link");
  private final Rule documentationRule          = combined(
    documentationCodeRule, documentationReferenceRule, documentationLinkRule);

  private final Rule blockCommentBeginPunctuationRule = scope(
    conditional(blockCommentBegin), "punctuation.definition.comment.begin");
  private final Rule blockCommentEndPunctuationRule   =
    scope(conditional(blockCommentEnd), "punctuation.definition.comment.end");
  private final Rule blockCommentRule                 = name(inner(scope(
    delimitated(captureSimple(blockCommentBeginPunctuationRule),
      captureSimple(blockCommentEndPunctuationRule)),
    "comment.block.documentation"), documentationRule));

  private final Rule lineCommentIndicatorPunctuationRule =
    scope(conditional(lineCommentIndicator),
      "punctuation.definition.comment.indicator");
  private final Rule lineCommentRule                     =
    Rule.name(Rule.inner(scope(
      delimitated(captureSimple(lineCommentIndicatorPunctuationRule), end()),
      "comment.line.number-sign"), documentationRule));

  private final Rule commentRule = combined(blockCommentRule, lineCommentRule);

  private Generator() {}

  private Grammar grammar() {
    return Grammar.combined("Thrice", "tr", numberRule, commentRule);
  }

  private Rule documentationRule(Pattern begin, Pattern end, String name) {
    return scope(delimitated(begin, end),
      "keyword.other.documentation." + name);
  }

  private Rule numberRule(Pattern pattern, String name) {
    return name(scope(conditional(pattern), "constant.numeric." + name));
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
