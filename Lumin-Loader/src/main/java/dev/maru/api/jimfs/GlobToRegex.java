package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.PatternSyntaxException;

final class GlobToRegex {
    private static final InternalCharMatcher REGEX_RESERVED = InternalCharMatcher.anyOf("^$.?+*\\[]{}()");
    private final String glob;
    private final String separators;
    private final InternalCharMatcher separatorMatcher;
    private final StringBuilder builder = new StringBuilder();
    private final Deque<State> states = new ArrayDeque<State>();
    private int index;
    private static final State NORMAL = new State() {

        @Override
        void process(GlobToRegex converter, char c) {
            switch (c) {
                case '?': {
                    converter.appendQuestionMark();
                    return;
                }
                case '[': {
                    converter.appendBracketStart();
                    converter.pushState(BRACKET_FIRST_CHAR);
                    return;
                }
                case '{': {
                    converter.appendCurlyBraceStart();
                    converter.pushState(CURLY_BRACE);
                    return;
                }
                case '*': {
                    converter.pushState(STAR);
                    return;
                }
                case '\\': {
                    converter.pushState(ESCAPE);
                    return;
                }
            }
            converter.append(c);
        }

        public String toString() {
            return "NORMAL";
        }
    };
    private static final State ESCAPE = new State() {

        @Override
        void process(GlobToRegex converter, char c) {
            converter.append(c);
            converter.popState();
        }

        @Override
        void finish(GlobToRegex converter) {
            throw converter.syntaxError("Hanging escape (\\) at end of pattern");
        }

        public String toString() {
            return "ESCAPE";
        }
    };
    private static final State STAR = new State() {

        @Override
        void process(GlobToRegex converter, char c) {
            if (c == '*') {
                converter.appendStarStar();
                converter.popState();
            } else {
                converter.appendStar();
                converter.popState();
                converter.currentState().process(converter, c);
            }
        }

        @Override
        void finish(GlobToRegex converter) {
            converter.appendStar();
        }

        public String toString() {
            return "STAR";
        }
    };
    private static final State BRACKET_FIRST_CHAR = new State() {

        @Override
        void process(GlobToRegex converter, char c) {
            if (c == ']') {
                throw converter.syntaxError("Empty []");
            }
            if (c == '!') {
                converter.appendExact('^');
            } else if (c == '-') {
                converter.appendExact(c);
            } else {
                converter.appendInBracket(c);
            }
            converter.popState();
            converter.pushState(BRACKET);
        }

        @Override
        void finish(GlobToRegex converter) {
            throw converter.syntaxError("Unclosed [");
        }

        public String toString() {
            return "BRACKET_FIRST_CHAR";
        }
    };
    private static final State BRACKET = new State() {

        @Override
        void process(GlobToRegex converter, char c) {
            if (c == ']') {
                converter.appendBracketEnd();
                converter.popState();
            } else {
                converter.appendInBracket(c);
            }
        }

        @Override
        void finish(GlobToRegex converter) {
            throw converter.syntaxError("Unclosed [");
        }

        public String toString() {
            return "BRACKET";
        }
    };
    private static final State CURLY_BRACE = new State() {

        @Override
        void process(GlobToRegex converter, char c) {
            switch (c) {
                case '?': {
                    converter.appendQuestionMark();
                    break;
                }
                case '[': {
                    converter.appendBracketStart();
                    converter.pushState(BRACKET_FIRST_CHAR);
                    break;
                }
                case '{': {
                    throw converter.syntaxError("{ not allowed in subpattern group");
                }
                case '*': {
                    converter.pushState(STAR);
                    break;
                }
                case '\\': {
                    converter.pushState(ESCAPE);
                    break;
                }
                case '}': {
                    converter.appendCurlyBraceEnd();
                    converter.popState();
                    break;
                }
                case ',': {
                    converter.appendSubpatternSeparator();
                    break;
                }
                default: {
                    converter.append(c);
                }
            }
        }

        @Override
        void finish(GlobToRegex converter) {
            throw converter.syntaxError("Unclosed {");
        }

        public String toString() {
            return "CURLY_BRACE";
        }
    };

    public static String toRegex(String glob, String separators) {
        return new GlobToRegex(glob, separators).convert();
    }

    private GlobToRegex(String glob, String separators) {
        this.glob = Preconditions.checkNotNull(glob);
        this.separators = separators;
        this.separatorMatcher = InternalCharMatcher.anyOf(separators);
    }

    private String convert() {
        this.pushState(NORMAL);
        this.index = 0;
        while (this.index < this.glob.length()) {
            this.currentState().process(this, this.glob.charAt(this.index));
            ++this.index;
        }
        this.currentState().finish(this);
        return this.builder.toString();
    }

    private void pushState(State state) {
        this.states.push(state);
    }

    private void popState() {
        this.states.pop();
    }

    private State currentState() {
        return this.states.peek();
    }

    private PatternSyntaxException syntaxError(String desc) {
        throw new PatternSyntaxException(desc, this.glob, this.index);
    }

    private void appendExact(char c) {
        this.builder.append(c);
    }

    private void append(char c) {
        if (this.separatorMatcher.matches(c)) {
            this.appendSeparator();
        } else {
            this.appendNormal(c);
        }
    }

    private void appendNormal(char c) {
        if (REGEX_RESERVED.matches(c)) {
            this.builder.append('\\');
        }
        this.builder.append(c);
    }

    private void appendSeparator() {
        if (this.separators.length() == 1) {
            this.appendNormal(this.separators.charAt(0));
        } else {
            this.builder.append('[');
            for (int i = 0; i < this.separators.length(); ++i) {
                this.appendInBracket(this.separators.charAt(i));
            }
            this.builder.append("]");
        }
    }

    private void appendNonSeparator() {
        this.builder.append("[^");
        for (int i = 0; i < this.separators.length(); ++i) {
            this.appendInBracket(this.separators.charAt(i));
        }
        this.builder.append(']');
    }

    private void appendQuestionMark() {
        this.appendNonSeparator();
    }

    private void appendStar() {
        this.appendNonSeparator();
        this.builder.append('*');
    }

    private void appendStarStar() {
        this.builder.append(".*");
    }

    private void appendBracketStart() {
        this.builder.append('[');
        this.appendNonSeparator();
        this.builder.append("&&[");
    }

    private void appendBracketEnd() {
        this.builder.append("]]");
    }

    private void appendInBracket(char c) {
        if (c == '\\') {
            this.builder.append('\\');
        }
        this.builder.append(c);
    }

    private void appendCurlyBraceStart() {
        this.builder.append('(');
    }

    private void appendSubpatternSeparator() {
        this.builder.append('|');
    }

    private void appendCurlyBraceEnd() {
        this.builder.append(')');
    }

    private static abstract class State {
        private State() {
        }

        abstract void process(GlobToRegex var1, char var2);

        void finish(GlobToRegex converter) {
        }
    }
}
