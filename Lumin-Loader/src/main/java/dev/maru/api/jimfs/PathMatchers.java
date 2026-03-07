package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

final class PathMatchers {
    private PathMatchers() {
    }

    public static PathMatcher getPathMatcher(String syntaxAndPattern, String separators, ImmutableSet<PathNormalization> normalizations) {
        int syntaxSeparator = syntaxAndPattern.indexOf(58);
        Preconditions.checkArgument(syntaxSeparator > 0, "Must be of the form 'syntax:pattern': %s", (Object) syntaxAndPattern);
        String syntax = Ascii.toLowerCase(syntaxAndPattern.substring(0, syntaxSeparator));
        String pattern = syntaxAndPattern.substring(syntaxSeparator + 1);
        switch (syntax) {
            case "glob": {
                pattern = GlobToRegex.toRegex(pattern, separators);
            }
            case "regex": {
                return PathMatchers.fromRegex(pattern, normalizations);
            }
        }
        throw new UnsupportedOperationException("Invalid syntax: " + syntaxAndPattern);
    }

    private static PathMatcher fromRegex(String regex, Iterable<PathNormalization> normalizations) {
        return new RegexPathMatcher(PathNormalization.compilePattern(regex, normalizations));
    }

    @VisibleForTesting
    static final class RegexPathMatcher
            implements PathMatcher {
        private final Pattern pattern;

        private RegexPathMatcher(Pattern pattern) {
            this.pattern = Preconditions.checkNotNull(pattern);
        }

        @Override
        public boolean matches(Path path) {
            return this.pattern.matcher(path.toString()).matches();
        }

        public String toString() {
            return MoreObjects.toStringHelper(this).addValue(this.pattern).toString();
        }
    }
}
