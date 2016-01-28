package org.keycloak.adapters.saml.tomcat;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.keycloak.adapters.saml.AbstractSamlAuthenticatorValve;
import org.keycloak.adapters.saml.CatalinaSamlSessionStore;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.tomcat.CatalinaUserSessionManagement;
import org.keycloak.adapters.tomcat.GenericPrincipalFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Tomcat8SamlSessionStore extends CatalinaSamlSessionStore {
    public Tomcat8SamlSessionStore(CatalinaUserSessionManagement sessionManagement, GenericPrincipalFactory principalFactory, SessionIdMapper idMapper, Request request, AbstractSamlAuthenticatorValve valve, HttpFacade facade, SamlDeployment deployment) {
        super(sessionManagement, principalFactory, idMapper, request, valve, facade, deployment);
    }

    @Override
    protected String changeSessionId(Session session) {
        Request request = this.request;
        if (!deployment.turnOffChangeSessionIdOnLogin()) return request.changeSessionId();
        else return session.getId();
    }
}
