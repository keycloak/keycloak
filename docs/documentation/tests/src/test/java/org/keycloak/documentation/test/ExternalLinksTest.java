package org.keycloak.documentation.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.documentation.test.utils.LinkUtils;

import java.io.IOException;
import java.util.List;

public class ExternalLinksTest {

    @ParameterizedTest
    @MethodSource("org.keycloak.documentation.test.Guides#guides()")
    public void checkExternalLinks(String guideName) throws IOException {
        Guide guide = new Guide(guideName);
        List<LinkUtils.InvalidLink> invalidLinks = LinkUtils.getInstance().findInvalidLinks(guide);
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
