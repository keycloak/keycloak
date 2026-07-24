package org.keycloak.documentation.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.documentation.test.utils.DocUtils;
import org.keycloak.documentation.test.utils.LinkUtils;

import java.io.IOException;
import java.util.Set;

public class GuideTest {

    @ParameterizedTest
    @MethodSource("org.keycloak.documentation.test.Guides#guides()")
    public void checkVariables(String guideName) throws IOException {
        Set<String> missingVariables = DocUtils.findMissingVariables(new Guide(guideName));
        checkFailures("Variables not found in guide " + guideName, missingVariables);
    }

    @ParameterizedTest
    @MethodSource("org.keycloak.documentation.test.Guides#guides()")
    public void checkIncludes(String guideName) throws IOException {
        Set<String> missingIncludes = DocUtils.findMissingIncludes(new Guide(guideName));
        checkFailures("Includes not found in guide " + guideName, missingIncludes);
    }

    @ParameterizedTest
    @MethodSource("org.keycloak.documentation.test.Guides#guides()")
    public void checkImages(String guideName) throws IOException {
        Set<String> failures = LinkUtils.getInstance().findInvalidImages(new Guide(guideName));
        checkFailures("Images not found in guide " + guideName, failures);
    }

    @ParameterizedTest
    @MethodSource("org.keycloak.documentation.test.Guides#guides()")
    public void checkInternalAnchors(String guideName) throws IOException {
        Set<String> invalidInternalAnchors = LinkUtils.getInstance().findInvalidInternalAnchors(new Guide(guideName));
        checkFailures("Internal anchors not found in guide " + guideName, invalidInternalAnchors);
    }

    private void checkFailures(String message, Set<String> failures) {
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(message + " (" + failures.size() + "):");

            for (String f : failures) {
                sb.append("\n\t\t- " + f);
            }

            Assertions.fail(sb.toString());
        }
    }
}
