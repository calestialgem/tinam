package thrice.tinam;

import static tinam.Rule.*;
import static tinam.Pattern.*;

import tinam.Pattern;
import tinam.Rule;
import tinam.Writer;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import tinam.Grammar;

public final class Generator {
  public static void generate(OutputStream output) {
    Writer.write(new OutputStreamWriter(output), new Generator().grammar());
  }

  private final Rule documentation = unconditional(
    combined(delimitated(scoped("keyword.other"), all("`"), all("`")),
      delimitated(scoped("keyword.other"), all("["), all("]")),
      delimitated(scoped("keyword.other"), all("<"), all(">"))));
  private final Rule comment       = unconditional(combined(
    delimitated(data("comment.block.documentation", documentation), all("#{"),
      all("}#")),
    delimitated(data("comment.line", documentation), all("#"), end())));

  private final Rule loneKeyword = conditional(empty(),
    or(inline("keyword.other", all("import")),
      inline("keyword.other", all("entrypoint")),
      inline("keyword.control", all("if")),
      inline("keyword.control", all("else")),
      inline("keyword.control", all("for")),
      inline("keyword.control", all("while")),
      inline("keyword.control", all("do")),
      inline("keyword.control", all("switch")),
      inline("keyword.control", all("case")),
      inline("keyword.control", all("default")),
      inline("keyword.control", all("fallthrough")),
      inline("keyword.control", all("break")),
      inline("keyword.control", all("continue")),
      inline("keyword.control", all("return")),
      inline("storage.type", all("var")), inline("storage.type", all("func")),
      inline("storage.type", all("interface")),
      inline("storage.type", all("struct")),
      inline("storage.type", all("enum")), inline("storage.type", all("union")),
      inline("storage.modifier", all("opaque")),
      inline("storage.modifier", all("discard")),
      inline("storage.modifier", all("noreturn")),
      inline("storage.modifier", all("mutable")),
      inline("storage.modifier", all("shared")),
      inline("storage.modifier", all("volatile")),
      inline("storage.modifier", all("alignas")),
      inline("storage.modifier", all("threadlocal"))));

  private final Pattern keywordName = or(all("import"), all("entrypoint"),
    all("if"), all("else"), all("for"), all("while"), all("do"), all("switch"),
    all("case"), all("default"), all("fallthrough"), all("break"),
    all("continue"), all("return"), all("var"), all("func"), all("interface"),
    all("struct"), all("enum"), all("union"), all("opaque"), all("discard"),
    all("noreturn"), all("mutable"), all("shared"), all("volatile"),
    all("alignas"), all("threadlocal"));

  private final Pattern identifier = separate(or(and(keywordName, all("_")),
    and(or(range('a', 'z'), range('A', 'Z')),
      zeroOrMore(or(range('a', 'z'), range('A', 'Z'), range('0', '9'))),
      notAfter(keywordName))));

  private final Conditional number = conditional(scoped("constant.numeric"),
    or(number(range('0', '9'), one("eE")),
      indicatedNumber(one("dD"), range('0', '9'), one("eE")),
      indicatedNumber(one("xX"),
        or(range('a', 'f'), range('A', 'F'), range('0', '9')), one("pP")),
      indicatedNumber(one("oO"), range('0', '7'), one("pP")),
      indicatedNumber(one("bB"), range('0', '1'), one("pP"))));

  private final Rule variableDefinition =
    conditional(scoped("meta.variable-definition"),
      relaxed(inline("entity.name.type", or(all("var"), identifier)),
        inline("variable.other.definition", identifier)));

  private final Rule type = conditional(scoped("meta.type"),
    relaxed(inline("entity.name.type", identifier), before(all("{"))));

  private final Rule call     = conditional(scoped("meta.call"),
    relaxed(inline("entity.name.function", identifier), before(all("("))));
  private final Rule property =
    conditional(scoped("variable.other.constant.property"),
      relaxed(after(or(all("."), all("::"))), identifier));
  private final Rule variable =
    conditional(scoped("variable.other.constant"), identifier);

  private final Rule operator = conditional(scoped("keyword.operator"),
    or(and(one("^*/+-&|!<>="), optional(all("="))), one("?:")));

  private final Rule punctuationDefinition =
    conditional(scoped("punctuation.definition"), one("(){}[]"));
  private final Rule punctuationSeparator  =
    conditional(scoped("punctuation.separator"), all(","));
  private final Rule punctuationAccessor   =
    conditional(scoped("punctuation.accessor"), or(all("."), all("::")));

  private final Rule string =
    delimitated(
      data("string.quoted.double",
        conditional(scoped("constant.character.escape"),
          or(and(all("\\"),
            repeat(or(range('0', '9'), range('a', 'f'), range('A', 'F')), 1,
              8)),
            all("\\\""), all("\\\\"))),
        conditional(scoped("invalid.illegal"), all("\\"))),
      all("\""), all("\""));

  private final Rule rawString = delimitated(
    data("string.quoted.other",
      conditional(scoped("constant.character.escape"), all("``"))),
    all("`"), and(all("`"), notBefore(all("`"))));

  private final Rule character =
    delimitated(data("constant.character",
      conditional(scoped("constant.character.escape"),
        or(and(all("\\"),
          repeat(or(range('0', '9'), range('a', 'f'), range('A', 'F')), 1, 8)),
          all("\\\'"), all("\\\\"))),
      conditional(scoped("invalid.illegal"), all("\\"))), all("'"), all("'"));

  private Generator() {}

  private Grammar grammar() {
    return Grammar.of("Thrice", "tr",
      List.of(comment, number, operator, punctuationSeparator,
        punctuationDefinition, punctuationAccessor, string, rawString,
        character, variableDefinition, type, call, property, variable,
        loneKeyword),
      Map.ofEntries(Map.entry(documentation, "documentation")));
  }

  private Pattern indicatedNumber(Pattern indicator, Pattern digit,
    Pattern exponentIndicator) {
    return separate(
      and(all("0"), indicator, numberBody(digit, exponentIndicator)));
  }

  private Pattern number(Pattern digit, Pattern exponentIndicator) {
    return separate(numberBody(digit, exponentIndicator));
  }

  private Pattern numberBody(Pattern digit, Pattern exponentIndicator) {
    return and(plainNumber(digit), optional(and(all("."), plainNumber(digit))),
      optional(and(exponentIndicator, optional(one("+-")),
        plainNumber(range('0', '9')))));
  }

  private Pattern plainNumber(Pattern digit) {
    return and(digit, zeroOrMore(and(optional(one("'")), digit)));
  }

  private Pattern separate(Pattern word) {
    var alphanumeric = or(range('a', 'z'), range('A', 'Z'), range('0', '9'));
    return and(notAfter(alphanumeric), word, notBefore(alphanumeric));
  }

  private Pattern relaxed(Pattern... sequence) {
    var relaxed = new ArrayList<Pattern>();
    if (sequence.length > 0) {
      var whitespace = zeroOrMore(all(" "));
      relaxed.add(sequence[0]);
      for (var i = 1; i < sequence.length; i++) {
        relaxed.add(whitespace);
        relaxed.add(sequence[i]);
      }
    }
    return and(Collections.unmodifiableList(relaxed));
  }

  private Pattern inline(String scope, Pattern pattern) {
    return capture(pattern, unconditional(scoped(scope)));
  }
}
