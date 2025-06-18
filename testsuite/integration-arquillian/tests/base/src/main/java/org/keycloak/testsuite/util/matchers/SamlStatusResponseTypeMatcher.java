/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.util.matchers;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.*;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author hmlnarik
 */
public class SamlStatusResponseTypeMatcher extends BaseMatcher<SAML2Object> {

    private final List<Matcher<URI>> statusMatchers;

    public SamlStatusResponseTypeMatcher(URI... statusMatchers) {
        this.statusMatchers = new ArrayList(statusMatchers.length);
        for (URI uri : statusMatchers) {
            this.statusMatchers.add(is(uri));
        }
    }

    public SamlStatusResponseTypeMatcher(List<Matcher<URI>> statusMatchers) {
        this.statusMatchers = statusMatchers;
    }

    @Override
    public boolean matches(Object item) {
        StatusCodeType statusCode = ((StatusResponseType) item).getStatus().getStatusCode();
        for (Matcher<URI> statusMatcher : statusMatchers) {
            if (! statusMatcher.matches(statusCode.getValue())) {
                return false;
            }
            statusCode = statusCode.getStatusCode();
        }
        return true;
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        StatusCodeType statusCode = ((StatusResponseType) item).getStatus().getStatusCode();
        description.appendText("was ");
        while (statusCode != null) {
            description.appendText("/").appendValue(statusCode.getValue());
            statusCode = statusCode.getStatusCode();
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("SAML status response status matches ");
        for (Matcher<URI> statusMatcher : statusMatchers) {
            description.appendText("/").appendDescriptionOf(statusMatcher);
        }
    }
}
