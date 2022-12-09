package thrice.tinam;

import static tinam.Rule.*;
import static tinam.Pattern.*;

import tinam.Pattern;
import tinam.Rule;
import tinam.Writer;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import tinam.Grammar;

public final class Generator {
  public static void generate(OutputStream output) {
    Writer.write(new OutputStreamWriter(output), new Generator().grammar());
  }

  private final Pattern lowercase    = range('a', 'z');
  private final Pattern uppercase    = range('A', 'Z');
  private final Pattern letter       = or(List.of(lowercase, uppercase));
  private final Pattern decimal      = range('0', '9');
  private final Pattern alphanumeric = or(List.of(letter, decimal));

  private final Pattern fractionSeparator = all(".");
  private final Pattern digitSeparator    = all("'");
  private final Pattern sign              = one("+-");
  private final Pattern numberIndicator   = all("0");

  private final Pattern decimalExponentIndicator = one("eE");
  private final Pattern decimalNumber            =
    separate(numberBody(decimal, decimalExponentIndicator));

  private final Pattern hexadecimal                  =
    or(List.of(decimal, range('a', 'f'), range('A', 'F')));
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
  private final Rule numberRule            =
    unconditional(combined(List.of(decimalNumberRule, hexadecimalNumberRule,
      octalNumberRule, binaryNumberRule)));

  private final Rule documentationCodeRule      = documentationRule(
    documentationCodeDeliminator, documentationCodeDeliminator, "code");
  private final Rule documentationReferenceRule = documentationRule(
    documentationReferenceBegin, documentationReferenceEnd, "reference");
  private final Rule documentationLinkRule      =
    documentationRule(documentationLinkBegin, documentationLinkEnd, "link");
  private final Rule documentationRule          =
    unconditional(combined(List.of(documentationCodeRule,
      documentationReferenceRule, documentationLinkRule)));

  private final Conditional blockCommentBeginPunctuationRule = conditional(
    scoped("punctuation.definition.comment.begin"), blockCommentBegin);
  private final Conditional blockCommentEndPunctuationRule   =
    conditional(scoped("punctuation.definition.comment.end"), blockCommentEnd);
  private final Rule        blockCommentRule                 =
    delimitated(data("comment.block.documentation", List.of(documentationRule)),
      capture(blockCommentBeginPunctuationRule),
      capture(blockCommentEndPunctuationRule));

  private final Conditional lineCommentIndicatorPunctuationRule = conditional(
    scoped("punctuation.definition.comment.indicator"), lineCommentIndicator);
  private final Rule        lineCommentRule                     =
    delimitated(data("comment.line.number-sign", List.of(documentationRule)),
      capture(lineCommentIndicatorPunctuationRule), end());

  private final Rule commentRule =
    unconditional(combined(List.of(blockCommentRule, lineCommentRule)));

  private Generator() {}

  private Grammar grammar() {
    return Grammar.of("Thrice", "tr", List.of(numberRule, commentRule),
      Map.ofEntries(Map.entry(numberRule, "number"),
        Map.entry(documentationRule, "documentation")));
  }

  private Rule documentationRule(Pattern initializer, Pattern terminator,
    String name) {
    return delimitated(scoped("keyword.other.documentation." + name),
      initializer, terminator);
  }

  private Rule numberRule(Pattern condition, String name) {
    return conditional(scoped("constant.numeric." + name), condition);
  }

  private Pattern separate(Pattern separated) {
    return and(
      List.of(notAfter(alphanumeric), separated, notBefore(alphanumeric)));
  }

  private Pattern indicatedNumber(Pattern indicator, Pattern digit,
    Pattern exponentIndicator) {
    return and(List.of(numberIndicator, indicator,
      numberBody(digit, exponentIndicator)));
  }

  private Pattern numberBody(Pattern digit, Pattern exponentIndicator) {
    return and(List.of(plainNumber(digit),
      optional(and(List.of(fractionSeparator, plainNumber(digit)))),
      optional(and(
        List.of(exponentIndicator, optional(sign), plainNumber(decimal))))));
  }

  private Pattern plainNumber(Pattern digit) {
    return and(List.of(digit,
      zeroOrMore(and(List.of(optional(digitSeparator), digit)))));
  }
}
