package dev.maru.api.jimfs;

import com.google.common.base.Ascii;
import com.google.common.base.Function;
import com.ibm.icu.lang.UCharacter;

import java.text.Normalizer;
import java.util.regex.Pattern;

public enum PathNormalization implements Function<String, String> {
    NONE(0) {
        @Override
        public String apply(String string) {
            return string;
        }
    },
    NFC(128) {
        @Override
        public String apply(String string) {
            return Normalizer.normalize(string, Normalizer.Form.NFC);
        }
    },
    NFD(128) {
        @Override
        public String apply(String string) {
            return Normalizer.normalize(string, Normalizer.Form.NFD);
        }
    },
    CASE_FOLD_UNICODE(66) {
        @Override
        public String apply(String string) {
            try {
                return UCharacter.foldCase((String) string, (boolean) true);
            } catch (NoClassDefFoundError e) {
                NoClassDefFoundError error = new NoClassDefFoundError("PathNormalization.CASE_FOLD_UNICODE requires ICU4J. Did you forget to include it on your classpath?");
                error.initCause(e);
                throw error;
            }
        }
    },
    CASE_FOLD_ASCII(2) {
        @Override
        public String apply(String string) {
            return Ascii.toLowerCase(string);
        }
    };

    private final int patternFlags;

    private PathNormalization(int patternFlags) {
        this.patternFlags = patternFlags;
    }

    @Override
    public abstract String apply(String var1);

    public int patternFlags() {
        return this.patternFlags;
    }

    public static String normalize(String string, Iterable<PathNormalization> normalizations) {
        String result = string;
        for (PathNormalization normalization : normalizations) {
            result = normalization.apply(result);
        }
        return result;
    }

    public static Pattern compilePattern(String regex, Iterable<PathNormalization> normalizations) {
        int flags = 0;
        for (PathNormalization normalization : normalizations) {
            flags |= normalization.patternFlags();
        }
        return Pattern.compile(regex, flags);
    }
}
