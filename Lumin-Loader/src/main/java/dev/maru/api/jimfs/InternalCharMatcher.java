package dev.maru.api.jimfs;

import java.util.Arrays;

final class InternalCharMatcher {
    private final char[] chars;

    public static InternalCharMatcher anyOf(String chars) {
        return new InternalCharMatcher(chars);
    }

    private InternalCharMatcher(String chars) {
        this.chars = chars.toCharArray();
        Arrays.sort(this.chars);
    }

    public boolean matches(char c) {
        return Arrays.binarySearch(this.chars, c) >= 0;
    }
}
