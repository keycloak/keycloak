package org.keycloak.examples.authn.delegate.federation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.examples.authn.delegate.util.httpclient.BackChannelHttpResponse;
import org.keycloak.examples.authn.delegate.util.httpclient.HttpClientProviderWrapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.util.JsonSerialization;

public class DelegatedUserStorageProvider implements UserStorageProvider, UserLookupProvider {
    protected KeycloakSession session;
    protected ComponentModel model;
    protected Map<String, UserModel> loadedUsers = new HashMap<>();
    
    private static final Logger logger = Logger.getLogger(DelegatedUserStorageProvider.class);
    
    public DelegatedUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        UserModel adapter = loadedUsers.get(username);
        if (adapter == null) {
                adapter = createAdapter(realm, username);
                loadedUsers.put(username, adapter);
        }
        return adapter;
    }
      
    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }
    
    @Override
    public void close() {
    }
        
    protected UserModel createAdapter(RealmModel realm, String username) {
        // here ask external authentication server for user info by "username" in context of keycloak.
        // Conventionally, use the term "username" for user ID which both keycloak and external authentication server use to refer unique user in this context.
        
        // TODO: need to verify value of each items of configuration
        String userInfoUriString;
        if (model.getConfig().containsKey(DelegatedUserStorageProviderFactory.AS_USERINFO_URI)) {
            userInfoUriString = model.getConfig().getFirst(DelegatedUserStorageProviderFactory.AS_USERINFO_URI);
        } else {
            logger.warn("the external server's user info endpoint not specified.");
            return null;    
        }
        
        try {
            checkSslForBackEndCommunication(Boolean.valueOf(model.getConfig().getFirst(DelegatedUserStorageProviderFactory.IS_BACKEND_COMM_SSL_REQUIRED)), userInfoUriString);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return null;
        }
            
        logger.debug("loaded config : userinfo URI = " + userInfoUriString);
        
        HttpClientProviderWrapper httpClientProviderWrapper = new HttpClientProviderWrapper(session);
        BackChannelHttpResponse backChannelHttpResponse = null;
        try {
            backChannelHttpResponse = httpClientProviderWrapper.doPost(userInfoUriString, "userid=" + username);
            //backChannelHttpResponse = httpClientProviderWrapper.doGet(userInfoUriString + "?userid=" + username);
        } catch (IOException ioe) {
            logger.warn("failed for backchannel communication to get userinfo: summary=" + ioe.toString() + " detail=" + ioe.getMessage());
            return null;
        }
        int status = backChannelHttpResponse.getStatusCode();
        if (status != 204 && status != 200) {
            logger.warn("the userinfo endopoint of the external authentication server returned error status code: " + status);
            return null;
        }
        AuthenticatedUserInfo authorizedUserInfo;
        try {
            authorizedUserInfo = JsonSerialization.readValue(backChannelHttpResponse.getBody(), AuthenticatedUserInfo.class);
        } catch (IOException ioe) {
            logger.warn("JSON Serialization Error. summary=" + ioe.toString() + " detail=" + ioe.getMessage());
            return null;
        }
        logger.debug("username as user ID =" + authorizedUserInfo.getUserName());
        logger.debug("attributes =" + authorizedUserInfo.getAttributes());
        
        return new AbstractUserAdapter(session, realm, model) {
            @Override
            public String getUsername() {
                return authorizedUserInfo.getUserName();
            }
            @Override
            public Map<String, List<String>> getAttributes() {
                return authorizedUserInfo.getAttributes();
            }
            @Override
            public List<String> getAttribute(String name) {
                List<String> list = getAttributes().get(name);
                return (list != null) ? list: new ArrayList<String>();
            }
        };
    }
    
    private void checkSslForBackEndCommunication(boolean isSslRequired, String uri) throws Exception {
        URI uriInfo;
        try {
            uriInfo = new URI(uri);
        } catch (URISyntaxException e) {
            throw(e);
        }
        logger.debug("user storage spi provider: uri you try to access in backend = " + uri);
        logger.debug("user storage spi provider: uriInfo.getScheme.equals - https - = " + uriInfo.getScheme().equals("https"));
        logger.debug("user storage spi provider: isSslRequired = " + isSslRequired);
        if (!uriInfo.getScheme().equals("https") && isSslRequired) {
            throw new RuntimeException("TLS must be enabled on backchannel communications.");
        }
    }
    

}
