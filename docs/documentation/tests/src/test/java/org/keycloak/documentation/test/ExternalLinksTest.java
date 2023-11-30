package org.keycloak.documentation.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.documentation.test.utils.LinkUtils;

import java.io.IOException;
import java.util.List;

public class ExternalLinksTest {

    static LinkUtils linkUtils = LinkUtils.getInstance();

    @BeforeAll
    public static void setup() {
        linkUtils = LinkUtils.getInstance();
    }

    @AfterAll
    public static void close() {
        // this will save a cache with all verified links that will expire after one day.
        linkUtils.close();
    }

    @ParameterizedTest
    @MethodSource("org.keycloak.documentation.test.Guides#guides()")
    public void checkExternalLinks(String guideName) throws IOException {
        Guide guide = new Guide(guideName);
        List<LinkUtils.InvalidLink> invalidLinks = linkUtils.findInvalidLinks(guide);
        if (!invalidLinks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Broken links (" + invalidLinks.size() + ") in guide '" + guideName + "': ");

            for (LinkUtils.InvalidLink l : invalidLinks) {
                sb.append("\n\t\t- " + l.getLink() + " (" + l.getError() + ")");
            }

            Assertions.fail(sb.toString());
        }
    }

}
