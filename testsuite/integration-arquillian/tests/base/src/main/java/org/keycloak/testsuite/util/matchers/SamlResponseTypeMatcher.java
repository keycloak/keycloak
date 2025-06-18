/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.util.matchers;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import java.net.URI;
import org.hamcrest.*;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author hmlnarik
 */
public class SamlResponseTypeMatcher extends BaseMatcher<SAML2Object> {

    private final Matcher<URI> statusMatcher;

    public SamlResponseTypeMatcher(JBossSAMLURIConstants expectedStatus) {
        this.statusMatcher = is(expectedStatus.getUri());
    }

    public SamlResponseTypeMatcher(Matcher<URI> statusMatcher) {
        this.statusMatcher = statusMatcher;
    }

    @Override
    public boolean matches(Object item) {
        return statusMatcher.matches(((ResponseType) item).getStatus().getStatusCode().getValue());
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText("was ").appendValue(((ResponseType) item).getStatus().getStatusCode());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("SAML response status code matches ").appendDescriptionOf(this.statusMatcher);
    }


}
