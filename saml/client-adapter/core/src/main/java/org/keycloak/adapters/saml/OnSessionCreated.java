package org.keycloak.adapters.saml;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface OnSessionCreated {

    void onSessionCreated(SamlSession samlSession);
}
