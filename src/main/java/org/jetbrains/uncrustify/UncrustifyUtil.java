package org.jetbrains.uncrustify;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UncrustifyUtil {
    private static final Pattern VERSION_PATTERN = Pattern.compile("Uncrustify(_d)?-((\\d+)\\.(\\d+)\\.(\\d+))(_[a-z])?$");

    public static @Nullable String validateVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        return matcher.find() ? matcher.group() : null;
    }
}
