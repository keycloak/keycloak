package org.keycloak.admin.client.service;

import org.codehaus.jackson.type.TypeReference;
import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.URI;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.adapters.action.SessionStats;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class RealmService extends KeycloakService{

    public RealmService(Config config, TokenManager tokenManager){
        super(config, tokenManager);
    }

    public RealmRepresentation getRealm(){
        String uri = URI.REALM.build(config.getServerUrl(), config.getRealm());
        return http.get(uri).getTypedResponse(RealmRepresentation.class);
    }

    public void logoutAllSessions(){
        String uri = URI.LOGOUT_ALL.build(config.getServerUrl(), config.getRealm());
        http.post(uri).execute();
    }

    public Map<String, SessionStats> getSessionStats(){
        String uri = URI.SESSION_STATS.build(config.getServerUrl(), config.getRealm());
        return http.get(uri).getTypedResponse(new TypeReference<Map<String, SessionStats>>(){});
    }

    public void removeUserSession(String sessionId){
        String uri = URI.USER_SESSION.build(config.getServerUrl(), config.getRealm(), sessionId);
        http.delete(uri).execute();
    }

}
