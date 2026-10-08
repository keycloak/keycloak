package org.keycloak.services.ui.extend;

import java.util.regex.Pattern;

public final class UiScriptProviderUtils {

    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("[a-z][a-z0-9]*(-[a-z0-9]+)+");

    private UiScriptProviderUtils() {
    }

    public static void validateTagName(String tagName) {
        if (tagName == null || !TAG_NAME_PATTERN.matcher(tagName).matches()) {
            throw new IllegalArgumentException("Invalid custom element tag name: " + tagName);
        }
    }

    public static void validateScriptPath(String scriptPath) {
        if (scriptPath == null || scriptPath.isEmpty() || scriptPath.startsWith("/") || scriptPath.contains("..")) {
            throw new IllegalArgumentException("Invalid script path: " + scriptPath);
        }
    }
}
