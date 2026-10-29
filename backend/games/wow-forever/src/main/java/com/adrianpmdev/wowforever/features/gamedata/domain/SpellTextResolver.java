package com.adrianpmdev.wowforever.features.gamedata.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the tokens of a spell text into what the game shows for a character of a given level with no gear: values of
 * effects ({@code $s1}, {@code $12345m2}), durations ({@code $d}), formulas ({@code ${$m1/-10}.1}), named values
 * ({@code $<damage>}), choices that depend on the character ({@code $?s12345[...][...]}, which take the branch of a
 * character without that spell or aura) and texts of other spells ({@code @spelltooltip12345}).
 * <p>
 * A talent or perk rank sets the values of some effects of its own spell (the client gives the value of each rank),
 * so the same text reads "by 2%" at rank 1 and "by 6%" at rank 3.
 * Anything it cannot read is kept as written, so a new kind of token shows up in the text instead of breaking it.
 *
 * @author adrianpmdev
 */
public final class SpellTextResolver {

    private static final int MAX_DEPTH = 4;
    private static final double EPSILON = 1e-6;

    /** Values that depend on gear or stats, which a character with no gear does not have. */
    private static final Set<String> GEAR_VALUES = Set.of(
            "ap", "AP", "rap", "RAP", "sp", "SP", "sps", "SPS", "spfr", "SPFR", "spfi", "SPFI", "spn", "SPN", "sph",
            "SPH", "spa", "SPA", "spsh", "SPSH", "mw", "MW", "mwb", "MWB", "mws", "MWS", "rwb", "RWB", "bh", "bc",
            "pbh", "pbhd", "hnd", "HND", "spi", "SPI", "sta", "STA", "str", "STR", "agi", "AGI", "int", "INT", "mhp",
            "MHP");
    private static final Set<String> LEVEL_VALUES = Set.of("pl", "PL");
    private static final Pattern PLURAL = Pattern.compile("[lLgG]([^:;$\\[\\]]*):([^;$\\[\\]]*);");
    private static final Pattern UNMET_REQUIREMENT = Pattern.compile("(?is)\\|cffff2020.*\\|r");
    /** Colours, textures and links of the game's text markup. */
    private static final Pattern MARKUP = Pattern.compile(
            "\\|c[0-9a-f]{8}|\\|r|\\|T[^|]*\\|t|\\|H[^|]*\\|h|\\|h", Pattern.CASE_INSENSITIVE);

    private final SpellLookup lookup;
    private final int playerLevel;

    /**
     * Builds the resolver.
     *
     * @param lookup      finds the spells that a text refers to
     * @param playerLevel level of the character the texts are read for
     */
    public SpellTextResolver(SpellLookup lookup, int playerLevel) {
        this.lookup = lookup;
        this.playerLevel = playerLevel;
    }

    /**
     * The description of a spell with its own values.
     *
     * @param spell spell
     * @return the text as the game shows it
     */
    public String description(SpellInfo spell) {
        return description(spell, Map.of());
    }

    /**
     * The description of a spell at one rank of a talent or perk.
     *
     * @param spell  spell
     * @param points value of each effect at that rank, by effect index (effects not listed keep their own value)
     * @return the text as the game shows it
     */
    public String description(SpellInfo spell, Map<Integer, Double> points) {
        return resolve(spell.description(), spell, points, 0);
    }

    /**
     * The text of the buff or debuff of a spell.
     *
     * @param spell spell
     * @return the text as the game shows it
     */
    public String auraDescription(SpellInfo spell) {
        return resolve(spell.auraDescription(), spell, Map.of(), 0);
    }

    private String resolve(String text, SpellInfo spell, Map<Integer, Double> points, int depth) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String resolved = new Parser(text, spell, points, depth).readText((char) 0);

        return MARKUP.matcher(resolved).replaceAll("").replace("\r\n", "\n").trim();
    }

    /** A value read from a token: its number (used in formulas) and how the text shows it. */
    private record Value(double number, String text) {

        /** A value as it is, decimals included: 3, 0.5. */
        static Value of(double number) {
            return new Value(number, TooltipFormat.text(Math.abs(number)));
        }

        /** A value rounded to a whole number: 17.25 reads 17. */
        static Value whole(double number) {
            return new Value(number, TooltipFormat.whole(Math.abs(number)));
        }
    }

    /** Reads one text, with a cursor over it. */
    private final class Parser {

        private final String text;
        private final SpellInfo spell;
        private final Map<Integer, Double> points;
        private final int depth;
        private Map<String, Double> variables = new HashMap<>();
        private int position;
        private double lastNumber = Double.NaN;
        private boolean variablesRead;
        /** Formulas keep every decimal: the text is a named value, not shown as it is. */
        private boolean exact;

        Parser(String text, SpellInfo spell, Map<Integer, Double> points, int depth) {
            this.text = text;
            this.spell = spell;
            this.points = points;
            this.depth = depth;
        }

        /* ─── Text ─── */

        /** Reads text until the closing character (not consumed), or to the end when it is 0. */
        String readText(char closing) {
            StringBuilder out = new StringBuilder();

            while (position < text.length()) {
                char character = text.charAt(position);

                if (closing != 0 && character == closing) {
                    break;
                }

                if (character == '[') {
                    position++;
                    out.append('[').append(readText(']'));

                    if (position < text.length()) {
                        out.append(']');
                        position++;
                    }

                    continue;
                }

                if (character == '$') {
                    out.append(readToken());
                    continue;
                }

                out.append(character);
                position++;
            }

            return out.toString();
        }

        private String readToken() {
            int start = position;
            position++;

            if (position >= text.length()) {
                return "$";
            }

            char next = text.charAt(position);

            try {
                return switch (next) {
                    case '{' -> readFormula();
                    case '?' -> readChoice();
                    case '@' -> readEmbedded();
                    case '<' -> show(readVariable());
                    case '/', '*' -> show(readScaled());
                    default -> readPlainToken(start);
                };
            } catch (IllegalStateException | IllegalArgumentException | IndexOutOfBoundsException exception) {
                position = start + 1;

                return "$";
            }
        }

        private String show(Value value) {
            lastNumber = value.number();

            return value.text();
        }

        private String readPlainToken(int start) {
            Matcher plural = PLURAL.matcher(text).region(position, text.length());

            if (plural.lookingAt()) {
                position = plural.end();
                char kind = text.charAt(start + 1);
                boolean singular = Character.toLowerCase(kind) == 'g'
                        || Math.abs(lastNumber - 1) < EPSILON;

                return singular ? plural.group(1) : plural.group(2);
            }

            Value value = readReference();

            if (value == null) {
                position = start + 1;

                return "$";
            }

            return show(value);
        }

        /* ─── References ─── */

        /** Reads {@code [spell id]letter[index]} or a named value after the {@code $}; null when it is neither. */
        private Value readReference() {
            int start = position;
            int spellId = readInteger();
            int lettersStart = position;

            while (position < text.length() && Character.isLetter(text.charAt(position))) {
                position++;
            }

            String letters = text.substring(lettersStart, position);

            if (letters.isEmpty()) {
                position = start;

                return null;
            }

            if (spellId == 0 && LEVEL_VALUES.contains(letters)) {
                return Value.of(playerLevel);
            }

            if (spellId == 0 && GEAR_VALUES.contains(letters)) {
                return Value.of(0);
            }

            if (spellId == 0 && "proccooldown".equals(letters)) {
                return Value.of(spell.procCooldownMs() / 1000.0);
            }

            position = lettersStart + 1;
            int index = readInteger();
            SpellInfo target = spellId == 0 ? spell : lookup.spell(spellId).orElse(null);

            if (target == null) {
                position = start;

                return null;
            }

            Map<Integer, Double> targetPoints = spellId == 0 ? points : Map.of();
            Value value = valueOf(target, targetPoints, letters.charAt(0), Math.max(1, index) - 1);

            if (value == null) {
                position = start;
            }

            return value;
        }

        private Value valueOf(SpellInfo target, Map<Integer, Double> targetPoints, char letter, int effectIndex) {
            SpellInfo.Effect effect = target.effect(effectIndex);

            return switch (letter) {
                case 's', 'S', 'm', 'w', 'W' -> effect == null ? null : points(target, effect, targetPoints);
                case 'M' -> effect == null ? null
                        : Value.whole(base(target, effect, targetPoints) * (1 + effect.variance() / 2));
                case 'o', 'O' -> effect == null ? null : total(target, effect, targetPoints);
                case 't', 'T' -> effect == null ? null : Value.of(effect.periodMs() / 1000.0);
                case 'a', 'A' -> Value.of(radius(target, effect));
                case 'x', 'X' -> effect == null ? null : Value.of(effect.chainTargets());
                case 'e', 'E' -> effect == null ? null : Value.of(effect.amplitude());
                case 'd', 'D' -> target.durationMs() == SpellInfo.UNTIL_CANCELLED
                        ? new Value(-1, TooltipFormat.duration(-1))
                        : seconds(target.durationMs() / 1000.0);
                case 'u', 'U' -> Value.of(target.stacks());
                case 'n', 'N' -> Value.of(target.charges());
                case 'h', 'H' -> Value.of(target.procChance());
                case 'r', 'R' -> Value.of(target.rangeYards());
                case 'c' -> new Value(0, lookup.cost(target.id()).orElse("0"));
                case 'i', 'I' -> Value.of(target.maxTargets());
                case 'b', 'B', 'q', 'v', 'V' -> Value.of(0);
                default -> null;
            };
        }

        private Value seconds(double seconds) {
            return new Value(seconds, TooltipFormat.duration(seconds));
        }

        /** The value of an effect: the rank's value when the rank sets it, otherwise the base value grown by level. */
        private double base(SpellInfo target, SpellInfo.Effect effect, Map<Integer, Double> targetPoints) {
            Double set = targetPoints.get(effect.index());

            if (set != null) {
                return set;
            }

            int topLevel = target.maxLevel() > 0 ? Math.min(playerLevel, target.maxLevel()) : playerLevel;

            return effect.basePoints() + effect.pointsPerLevel() * Math.max(0, topLevel - target.spellLevel());
        }

        /**
         * How a value shows: "421 to 439" when it has a spread (the whole numbers inside it), a whole number when it
         * grows by level, otherwise as the client gives it (0.5 stays 0.5).
         */
        private Value points(SpellInfo target, SpellInfo.Effect effect, Map<Integer, Double> targetPoints) {
            double base = base(target, effect, targetPoints);

            if (effect.variance() > 0) {
                double spread = Math.abs(base) * effect.variance() / 2;

                return new Value(base, TooltipFormat.whole(Math.ceil(Math.abs(base) - spread)) + " to "
                        + TooltipFormat.whole(Math.floor(Math.abs(base) + spread)));
            }

            return effect.pointsPerLevel() != 0 ? Value.whole(base) : Value.of(base);
        }

        /** {@code $o}: the value of every tick of a periodic effect added up over the duration. */
        private Value total(SpellInfo target, SpellInfo.Effect effect, Map<Integer, Double> targetPoints) {
            double tick = base(target, effect, targetPoints);
            double ticks = effect.periodMs() <= 0 || target.durationMs() <= 0 ? 1
                    : Math.floor((double) target.durationMs() / effect.periodMs());

            return Value.whole(tick * ticks);
        }

        /** {@code $a}: the radius of an effect, or of the first effect with one when the text names a missing one. */
        private double radius(SpellInfo target, SpellInfo.Effect effect) {
            if (effect != null && effect.radiusYards() > 0) {
                return effect.radiusYards();
            }

            return target.effects().stream().mapToDouble(SpellInfo.Effect::radiusYards).filter(radius -> radius > 0)
                    .findFirst().orElse(0);
        }

        /** {@code $/10;s1} and {@code $*2;s1}: a value divided or multiplied by a number. */
        private Value readScaled() {
            char operator = text.charAt(position);
            position++;
            int end = text.indexOf(';', position);

            if (end < 0) {
                throw new IllegalStateException("No end of scale");
            }

            double factor = Double.parseDouble(text.substring(position, end));
            position = end + 1;
            Value value = readReference();

            if (value == null) {
                throw new IllegalStateException("No value to scale");
            }

            return Value.of(operator == '/' ? value.number() / factor : value.number() * factor);
        }

        /** {@code $<name>}: a named value defined by the spell. */
        private Value readVariable() {
            int end = text.indexOf('>', position);

            if (end < 0) {
                throw new IllegalStateException("No end of variable");
            }

            String name = text.substring(position + 1, end);
            position = end + 1;
            Double value = variable(name);

            if (value == null) {
                throw new IllegalStateException("Unknown variable " + name);
            }

            return Value.of(value);
        }

        private Double variable(String name) {
            if (!variablesRead) {
                variablesRead = true;

                for (String line : spell.variables().split("\\R")) {
                    int equals = line.indexOf('=');

                    if (line.startsWith("$") && equals > 1) {
                        Parser parser = new Parser(line.substring(equals + 1).trim(), spell, points, depth);
                        parser.variables = variables;
                        parser.variablesRead = true;
                        parser.exact = true;
                        number(parser.readText((char) 0))
                                .ifPresent(value -> variables.put(line.substring(1, equals).trim(), value));
                    }
                }
            }

            return variables.get(name);
        }

        private static Optional<Double> number(String text) {
            try {
                return Optional.of(Double.parseDouble(text.replace(",", "").trim()));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

        /* ─── Formulas ─── */

        /** {@code ${expression}} with an optional {@code .N} for N decimals. */
        private String readFormula() {
            position++;
            double value = expression();
            expect('}');
            Integer decimals = null;

            if (position + 1 < text.length() && text.charAt(position) == '.'
                    && Character.isDigit(text.charAt(position + 1))) {
                decimals = text.charAt(position + 1) - '0';
                position += 2;
            }

            lastNumber = value;

            if (exact) {
                return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
            }

            return decimals == null ? TooltipFormat.whole(value) : TooltipFormat.number(value, decimals);
        }

        private double expression() {
            double value = comparison();

            while (true) {
                skipSpaces();

                if (consume("&&") || consume("&")) {
                    double right = comparison();
                    value = truth(value) && truth(right) ? 1 : 0;
                } else if (consume("||") || consume("|")) {
                    double right = comparison();
                    value = truth(value) || truth(right) ? 1 : 0;
                } else {
                    return value;
                }
            }
        }

        private double comparison() {
            double value = sum();
            skipSpaces();

            if (consume(">=")) {
                return value >= sum() ? 1 : 0;
            }

            if (consume("<=")) {
                return value <= sum() ? 1 : 0;
            }

            if (consume("==") || consume("=")) {
                return Math.abs(value - sum()) < EPSILON ? 1 : 0;
            }

            if (consume("!=")) {
                return Math.abs(value - sum()) >= EPSILON ? 1 : 0;
            }

            if (consume(">")) {
                return value > sum() ? 1 : 0;
            }

            if (consume("<")) {
                return value < sum() ? 1 : 0;
            }

            return value;
        }

        private double sum() {
            double value = product();

            while (true) {
                skipSpaces();

                if (consume("+")) {
                    value += product();
                } else if (consume("-")) {
                    value -= product();
                } else {
                    return value;
                }
            }
        }

        private double product() {
            double value = unary();

            while (true) {
                skipSpaces();

                if (consume("*")) {
                    value *= unary();
                } else if (consume("/")) {
                    double divisor = unary();
                    value = divisor == 0 ? 0 : value / divisor;
                } else {
                    return value;
                }
            }
        }

        private double unary() {
            skipSpaces();

            if (consume("-")) {
                return -unary();
            }

            if (consume("!")) {
                return truth(unary()) ? 0 : 1;
            }

            return operand();
        }

        private double operand() {
            skipSpaces();

            if (consume("(")) {
                double value = expression();
                expect(')');

                return value;
            }

            if (position >= text.length()) {
                throw new IllegalStateException("Formula ends early");
            }

            char character = text.charAt(position);

            if (Character.isDigit(character) || character == '.') {
                int start = position;

                while (position < text.length()
                        && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
                    position++;
                }

                return Double.parseDouble(text.substring(start, position));
            }

            if (character == '$') {
                return reference();
            }

            if (character == '?' || Character.isLetter(character)) {
                return condition();
            }

            throw new IllegalStateException("Unexpected " + character + " in a formula");
        }

        private double reference() {
            position++;

            if (position < text.length() && text.charAt(position) == '<') {
                return readVariable().number();
            }

            if (position < text.length() && (text.charAt(position) == '/' || text.charAt(position) == '*')) {
                return readScaled().number();
            }

            int start = position;

            while (position < text.length() && Character.isLetter(text.charAt(position))) {
                position++;
            }

            String name = text.substring(start, position).toLowerCase(Locale.ROOT);

            if (position < text.length() && text.charAt(position) == '(') {
                return function(name);
            }

            position = start;
            Value value = readReference();

            if (value == null) {
                throw new IllegalStateException("Unknown reference in a formula");
            }

            return value.number();
        }

        private double function(String name) {
            position++;
            double[] arguments = new double[4];
            int count = 0;
            skipSpaces();

            if (!consume(")")) {
                do {
                    double argument = expression();

                    if (count < arguments.length) {
                        arguments[count] = argument;
                    }

                    count++;
                    skipSpaces();
                } while (consume(","));

                expect(')');
            }

            return switch (name) {
                case "max" -> Math.max(arguments[0], arguments[1]);
                case "min" -> Math.min(arguments[0], arguments[1]);
                case "floor" -> Math.floor(arguments[0]);
                case "ceil" -> Math.ceil(arguments[0]);
                case "abs" -> Math.abs(arguments[0]);
                case "round" -> Math.round(arguments[0]);
                case "gt" -> arguments[0] > arguments[1] ? 1 : 0;
                case "gte" -> arguments[0] >= arguments[1] ? 1 : 0;
                case "lt" -> arguments[0] < arguments[1] ? 1 : 0;
                case "lte" -> arguments[0] <= arguments[1] ? 1 : 0;
                case "eq" -> Math.abs(arguments[0] - arguments[1]) < EPSILON ? 1 : 0;
                case "cond" -> truth(arguments[0]) ? arguments[1] : arguments[2];
                case "clamp" -> Math.max(arguments[1], Math.min(arguments[2], arguments[0]));
                default -> throw new IllegalStateException("Unknown function " + name);
            };
        }

        /* ─── Choices ─── */

        /**
         * {@code $?s123[then][else]}, chains of {@code ?s456[...]} and conditions joined by {@code |} or {@code &}.
         * The character is assumed to have none of the spells, auras or classes the condition names.
         */
        private String readChoice() {
            position++;

            while (true) {
                boolean holds = conditionUntilBranch();
                String chosen = holds ? branch() : skipBranch();

                if (holds) {
                    skipRemainingBranches();

                    return unmetRequirement(chosen) ? "" : chosen;
                }

                if (position < text.length() && text.charAt(position) == '?') {
                    position++;
                    continue;
                }

                if (position < text.length() && text.charAt(position) == '[') {
                    String otherwise = branch();

                    return unmetRequirement(otherwise) ? "" : otherwise;
                }

                return "";
            }
        }

        /**
         * A branch written all in red, the game's colour for a requirement the character does not meet ("Requires
         * Bear Form"): the tooltip of a talent or spell read on its own does not show it.
         */
        private static boolean unmetRequirement(String branch) {
            return UNMET_REQUIREMENT.matcher(branch.trim()).matches();
        }

        private boolean conditionUntilBranch() {
            int end = text.indexOf('[', position);

            if (end < 0) {
                throw new IllegalStateException("Choice without branches");
            }

            Parser parser = new Parser(text.substring(position, end), spell, points, depth);
            boolean holds = truth(parser.expression());
            position = end;

            return holds;
        }

        /** A condition word ({@code s123}, {@code a123}, {@code c3}...) inside a choice: the character has none. */
        private double condition() {
            consume("?");

            while (position < text.length() && Character.isLetterOrDigit(text.charAt(position))) {
                position++;
            }

            return 0;
        }

        private String branch() {
            expect('[');
            String value = readText(']');
            expect(']');

            return value;
        }

        private String skipBranch() {
            expect('[');
            int level = 1;

            while (position < text.length() && level > 0) {
                char character = text.charAt(position++);

                if (character == '[') {
                    level++;
                } else if (character == ']') {
                    level--;
                }
            }

            return "";
        }

        private void skipRemainingBranches() {
            while (position < text.length()) {
                char character = text.charAt(position);

                if (character == '[') {
                    skipBranch();
                } else if (character == '?' && text.indexOf('[', position) > position) {
                    position = text.indexOf('[', position);
                    skipBranch();
                } else {
                    return;
                }
            }
        }

        /* ─── Other spells ─── */

        /**
         * {@code $@spelltooltip123} (name, tooltip line and description: "Feral Charge (Cat): 8-25 yd range, Instant.
         * Leap behind an enemy."), {@code $@spelldesc123}, {@code $@spellaura123} and {@code $@spellname123}.
         */
        private String readEmbedded() {
            position++;
            int start = position;

            while (position < text.length() && Character.isLetter(text.charAt(position))) {
                position++;
            }

            String kind = text.substring(start, position);
            int spellId = readInteger();
            Optional<SpellInfo> target = lookup.spell(spellId);

            if (target.isEmpty() || depth >= MAX_DEPTH) {
                return "";
            }

            String description = resolve(target.get().description(), target.get(), Map.of(), depth + 1);

            return switch (kind) {
                case "spellname" -> target.get().name();
                case "spellaura" -> resolve(target.get().auraDescription(), target.get(), Map.of(), depth + 1);
                case "spelldesc" -> description;
                case "spelltooltip" -> tooltip(target.get(), description);
                default -> "";
            };
        }

        private String tooltip(SpellInfo target, String description) {
            List<String> line = lookup.tooltipLine(target.id());

            return line.isEmpty() ? description
                    : target.name() + ": " + String.join(", ", line) + ". " + description;
        }

        /* ─── Cursor ─── */

        private int readInteger() {
            int start = position;

            while (position < text.length() && Character.isDigit(text.charAt(position))) {
                position++;
            }

            return start == position ? 0 : Integer.parseInt(text.substring(start, position));
        }

        private void skipSpaces() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }

        private boolean consume(String token) {
            if (text.startsWith(token, position)) {
                position += token.length();

                return true;
            }

            return false;
        }

        private void expect(char character) {
            skipSpaces();

            if (position >= text.length() || text.charAt(position) != character) {
                throw new IllegalStateException("Expected " + character);
            }

            position++;
        }

        private static boolean truth(double value) {
            return Math.abs(value) >= EPSILON;
        }
    }
}
