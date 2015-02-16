package org.keycloak.services.managers;

import org.keycloak.login.LoginFormsProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface HttpAuthenticationChallenge {

    void addChallenge(LoginFormsProvider loginFormsProvider);
}
