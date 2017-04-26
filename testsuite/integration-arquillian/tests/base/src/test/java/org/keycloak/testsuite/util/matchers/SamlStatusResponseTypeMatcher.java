/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.util.matchers;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import java.net.URI;
import org.hamcrest.*;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author hmlnarik
 */
public class SamlStatusResponseTypeMatcher extends BaseMatcher<SAML2Object> {

    private final Matcher<URI> statusMatcher;

    public SamlStatusResponseTypeMatcher(URI statusMatcher) {
        this.statusMatcher = is(statusMatcher);
    }

    public SamlStatusResponseTypeMatcher(Matcher<URI> statusMatcher) {
        this.statusMatcher = statusMatcher;
    }

    @Override
    public boolean matches(Object item) {
        return statusMatcher.matches(((StatusResponseType) item).getStatus().getStatusCode().getValue());
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText("was ").appendValue(((StatusResponseType) item).getStatus().getStatusCode().getValue());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("SAML status response status matches ").appendDescriptionOf(this.statusMatcher);
    }
}
