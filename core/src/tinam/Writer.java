package tinam;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import tinam.Rule.*;
import tinam.Pattern.*;

public final class Writer {
  public static void write(OutputStreamWriter output, Grammar written) {
    new Writer(output, written).write();
  }

  private final OutputStreamWriter output;
  private final Grammar            written;

  private boolean objectStart;

  private List<Rule> captures;

  private Writer(OutputStreamWriter output, Grammar written) {
    this.output  = output;
    this.written = written;
  }

  private void write() {
    writeCharacter('{');

    writeString("name");
    writeCharacter(':');
    writeString(written.name());

    writeCharacter(',');
    writeString("scopeName");
    writeCharacter(':');
    writeString("source." + written.extension());

    var topLevelIterator = written.topLevel().iterator();
    if (topLevelIterator.hasNext()) {
      writeCharacter(',');
      writeString("patterns");
      writeCharacter(':');
      writeCharacter('[');
      accessObject(topLevelIterator.next());
      while (topLevelIterator.hasNext()) {
        writeCharacter(',');
        accessObject(topLevelIterator.next());
      }
      writeCharacter(']');
    }

    var repositoryIterator = written.repository().entrySet().iterator();
    if (repositoryIterator.hasNext()) {
      writeCharacter(',');
      writeString("repository");
      writeCharacter(':');
      writeCharacter('{');
      defineObject(repositoryIterator.next());
      while (repositoryIterator.hasNext()) {
        writeCharacter(',');
        defineObject(repositoryIterator.next());
      }
      writeCharacter('}');
    }

    writeCharacter('}');
    try {
      output.flush();
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  private void defineObject(Entry<Rule, String> namedRule) {
    writeString(namedRule.getValue());
    writeCharacter(':');
    object(namedRule.getKey());
  }

  private void accessObject(Rule rule) {
    if (!written.repository().containsKey(rule)) {
      object(rule);
      return;
    }
    writeCharacter('{');
    writeString("include");
    writeCharacter(':');
    writeString('#' + written.repository().get(rule));
    writeCharacter('}');
  }

  private void object(Rule rule) {
    objectStart = true;
    writeCharacter('{');
    if (rule.data().scope().isPresent()) {
      objectSeparate();

      writeString("name");
      writeCharacter(':');
      writeString(rule.data().scope().get() + '.' + written.extension());
    }
    switch (rule) {
    case Unconditional unconditional -> objectUnconditional();
    case Conditional conditional -> objectConditional(conditional);
    case Delimitated delimitated -> objectDelimitated(delimitated);
    }
    if (!rule.data().inner().isEmpty()) {
      objectSeparate();

      writeString("patterns");
      writeCharacter(':');
      writeCharacter('[');
      accessObject(rule.data().inner().get(0));
      for (var i = 1; i < rule.data().inner().size(); i++) {
        writeCharacter(',');
        accessObject(rule.data().inner().get(i));
      }
      writeCharacter(']');
    }
    writeCharacter('}');
  }

  private void objectUnconditional() {}

  private void objectConditional(Conditional conditional) {
    objectPattern("match", "captures", conditional.condition());
  }

  private void objectDelimitated(Delimitated delimitated) {
    objectPattern("begin", "beginCaptures", delimitated.initializer());
    objectPattern("end", "endCaptures", delimitated.terminator());
  }

  private void objectPattern(String name, String capturesName,
    Pattern pattern) {
    captures = new ArrayList<>();

    objectSeparate();

    writeString(name);
    writeCharacter(':');
    writeCharacter('"');
    regex(pattern);
    writeCharacter('"');

    if (captures.isEmpty()) return;
    writeCharacter(',');
    writeString(capturesName);
    writeCharacter(':');
    writeCharacter('{');
    objectCapture(0 + 1, captures.get(0));
    for (var i = 1; i < captures.size(); i++) {
      writeCharacter(',');
      objectCapture(i + 1, captures.get(i));
    }
    writeCharacter('}');
  }

  private void objectSeparate() {
    if (!objectStart) writeCharacter(',');
    objectStart = false;
  }

  private void objectCapture(int index, Rule captured) {
    writeString(String.valueOf(index));
    writeCharacter(':');
    object(captured);
  }

  private void unitRegex(Pattern pattern) {
    if ((pattern instanceof Or patternAsOr
      && surveyOr(patternAsOr) == Survey.OTHER) || pattern instanceof And)
      unitRegexSurround(pattern);
    else regex(pattern);
  }

  private void unitRegexSurround(Pattern surrounded) {
    writeEscaped('(');
    writeEscaped('?');
    writeEscaped(':');
    regex(surrounded);
    writeEscaped(')');
  }

  private void regex(Pattern pattern) {
    switch (pattern) {
    case One one -> regexOne(one);
    case NotOne notOne -> regexNotOne(notOne);
    case Range range -> regexRange(range);
    case NotRange notRange -> regexNotRange(notRange);
    case Any any -> regexAny();
    case All all -> regexAll(all);
    case Start start -> regexStart();
    case End end -> regexEnd();
    case Or or -> regexOr(or);
    case And and -> regexAnd(and);
    case Repeat repeat -> regexRepeat(repeat);
    case InfiniteRepeat infiniteRepeat -> regexInfiniteRepeat(infiniteRepeat);
    case Lookup lookup -> regexLookup(lookup);
    case Capture capture -> regexCapture(capture);
    }
  }

  private void regexOne(One one) {
    if (one.set().length() == 1) {
      regexLiteral(one.set());
    } else {
      writeEscaped('[');
      regexSet(one.set());
      writeEscaped(']');
    }
  }

  private void regexNotOne(NotOne notOne) {
    writeEscaped('[');
    writeEscaped('^');
    regexSet(notOne.set());
    writeEscaped(']');
  }

  private void regexRange(Range range) {
    writeEscaped('[');
    regexRange(range.first(), range.last());
    writeEscaped(']');
  }

  private void regexNotRange(NotRange notRange) {
    writeEscaped('[');
    writeEscaped('^');
    regexRange(notRange.first(), notRange.last());
    writeEscaped(']');
  }

  private void regexAny() { writeEscaped('.'); }

  private void regexAll(All all) { regexLiteral(all.characters()); }

  private void regexStart() { writeEscaped('^'); }

  private void regexEnd() { writeEscaped('$'); }

  private static enum Survey {
    SETS, NOT_SETS, OTHER;
  }

  private void regexOr(Or or) {
    var survey = surveyOr(or);

    switch (survey) {
    case SETS:
      writeEscaped('[');
      regexOrSets(or);
      writeEscaped(']');
      break;
    case NOT_SETS:
      writeEscaped('[');
      writeEscaped('^');
      regexOrNotSets(or);
      writeEscaped(']');
      break;
    default:
      regexOrAlternative(or.alternatives().get(0));
      for (var i = 1; i < or.alternatives().size(); i++) {
        writeEscaped('|');
        regexOrAlternative(or.alternatives().get(i));
      }
    }
  }

  private void regexOrSets(Or or) {
    for (var alternative : or.alternatives()) {
      switch (alternative) {
      case One one -> regexSet(one.set());
      case Range range -> regexRange(range.first(), range.last());
      case Or nestedOr -> regexOrSets(nestedOr);
      default ->
        throw new RuntimeException("Pattern type `%s` is not One or Range!"
          .formatted(alternative.getClass().getSimpleName()));
      }
    }
  }

  private void regexOrNotSets(Or or) {
    for (var alternative : or.alternatives()) {
      switch (alternative) {
      case NotOne notOne -> regexSet(notOne.set());
      case NotRange notRange -> regexRange(notRange.first(), notRange.last());
      case Or nestedOr -> regexOrNotSets(nestedOr);
      default -> throw new RuntimeException(
        "Pattern type `%s` is not NotOne or NotRange!"
          .formatted(alternative.getClass().getSimpleName()));
      }
    }
  }

  private void regexOrAlternative(Pattern alternative) {
    if (alternative instanceof Or alternativeAsOr) regexOr(alternativeAsOr);
    else unitRegex(alternative);
  }

  private Survey surveyOr(Or or) {
    var result = Survey.OTHER;

    for (var alternative : or.alternatives()) {
      switch (result) {
      case SETS:
        if (alternative instanceof One || alternative instanceof Range
          || (alternative instanceof Or alternativeAsOr
            && surveyOr(alternativeAsOr) == Survey.SETS))
          continue;
        break;

      case NOT_SETS:
        if (alternative instanceof NotOne || alternative instanceof NotRange
          || (alternative instanceof Or alternativeAsOr
            && surveyOr(alternativeAsOr) == Survey.NOT_SETS))
          continue;
        break;

      default:
        if (alternative instanceof One || alternative instanceof Range) {
          result = Survey.SETS;
          continue;
        }
        if (alternative instanceof NotOne || alternative instanceof NotRange) {
          result = Survey.NOT_SETS;
          continue;
        }
        if (alternative instanceof Or alternativeAsOr) {
          result = surveyOr(alternativeAsOr);
          if (result != Survey.OTHER) continue;
        }
      }
      result = Survey.OTHER;
      break;
    }

    return result;
  }

  private void regexAnd(And and) {
    for (var sequent : and.sequence())
      if (sequent instanceof And sequentAsAnd) regexAnd(sequentAsAnd);
      else unitRegex(sequent);
  }

  private void regexRepeat(Repeat repeat) {
    unitRegex(repeat.repeated());
    if (repeat.minimum() == 0 && repeat.maximum() == 1) {
      writeEscaped('?');
      return;
    }
    writeEscaped('{');
    writeNumber(repeat.minimum());
    writeEscaped(',');
    writeNumber(repeat.maximum());
    writeEscaped('}');
  }

  private void regexInfiniteRepeat(InfiniteRepeat infiniteRepeat) {
    unitRegex(infiniteRepeat.repeated());
    switch (infiniteRepeat.minimum()) {
    case 0:
      writeEscaped('*');
      break;
    case 1:
      writeEscaped('+');
      break;
    default:
      writeEscaped('{');
      writeNumber(infiniteRepeat.minimum());
      writeEscaped(',');
      writeEscaped('}');
    }
  }

  private void regexLookup(Lookup lookup) {
    writeEscaped('(');
    writeEscaped('?');
    if (lookup.behind()) writeEscaped('<');
    writeEscaped(lookup.wanted() ? '?' : '!');
    regex(lookup.looked());
    writeEscaped(')');
  }

  private void regexCapture(Capture capture) {
    writeEscaped('(');
    regex(capture.pattern());
    writeEscaped(')');
    captures.add(capture.rule());
  }

  private void regexSet(String set) {
    for (var member : set.toCharArray()) regexMember(member);
  }

  private void regexRange(char first, char last) {
    regexMember(first);
    writeEscaped('-');
    regexMember(last);
  }

  private void regexMember(char member) {
    switch (member) {
    case '\\', '^', '[', ']', '-':
      writeEscaped('\\');
      // $FALL-THROUGH$
    default:
      writeEscaped(member);
    }
  }

  private void regexLiteral(String literal) {
    for (var character : literal.toCharArray()) regexCharacter(character);
  }

  private void regexCharacter(char character) {
    switch (character) {
    case '\\', '^', '$', '[', ']', '(', ')', '{', '}', '.', '+', '*', '?', '!':
      writeEscaped('\\');
      // $FALL-THROUGH$
    default:
      writeEscaped(character);
    }
  }

  private void writeNumber(int number) {
    try {
      output.write(String.valueOf(number));
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  private void writeEscaped(char escaped) {
    try {
      switch (escaped) {
      case '\\', '"':
        output.write('\\');
        // $FALL-THROUGH$
      default:
        output.write(escaped);
      }
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  private void writeString(String string) {
    try {
      output.write('"');
      output.write(string);
      output.write('"');
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  private void writeCharacter(char character) {
    try {
      output.write(character);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }
}
