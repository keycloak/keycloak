package org.keycloak.services.offline;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.OfflineUserSessionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineUserSessionAdapter implements UserSessionModel {

    private final OfflineUserSessionModel model;
    private final UserModel user;

    private OfflineUserSessionData data;

    public OfflineUserSessionAdapter(OfflineUserSessionModel model, UserModel user) {
        this.model = model;
        this.user = user;
    }

    // lazily init representation
    private OfflineUserSessionData getData() {
        if (data == null) {
            try {
                data = JsonSerialization.readValue(model.getData(), OfflineUserSessionData.class);
            } catch (IOException ioe) {
                throw new ModelException(ioe);
            }
        }

        return data;
    }

    @Override
    public String getId() {
        return model.getUserSessionId();
    }

    @Override
    public String getBrokerSessionId() {
        return getData().getBrokerSessionId();
    }

    @Override
    public String getBrokerUserId() {
        return getData().getBrokerUserId();
    }

    @Override
    public UserModel getUser() {
        return user;
    }

    @Override
    public String getLoginUsername() {
        return user.getUsername();
    }

    @Override
    public String getIpAddress() {
        return getData().getIpAddress();
    }

    @Override
    public String getAuthMethod() {
        return getData().getAuthMethod();
    }

    @Override
    public boolean isRememberMe() {
        return getData().isRememberMe();
    }

    @Override
    public int getStarted() {
        return getData().getStarted();
    }

    @Override
    public int getLastSessionRefresh() {
        return 0;
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        // Ignore
    }

    @Override
    public List<ClientSessionModel> getClientSessions() {
        throw new IllegalStateException("Not yet supported");
    }

    @Override
    public String getNote(String name) {
        return getData().getNotes()==null ? null : getData().getNotes().get(name);
    }

    @Override
    public void setNote(String name, String value) {
        throw new IllegalStateException("Illegal to set note offline session");

    }

    @Override
    public void removeNote(String name) {
        throw new IllegalStateException("Illegal to remove note from offline session");
    }

    @Override
    public Map<String, String> getNotes() {
        return getData().getNotes();
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public void setState(State state) {
        throw new IllegalStateException("Illegal to set state on offline session");
    }


    protected static class OfflineUserSessionData {

        @JsonProperty("brokerSessionId")
        private String brokerSessionId;

        @JsonProperty("brokerUserId")
        private String brokerUserId;

        @JsonProperty("ipAddress")
        private String ipAddress;

        @JsonProperty("authMethod")
        private String authMethod;

        @JsonProperty("rememberMe")
        private boolean rememberMe;

        @JsonProperty("started")
        private int started;

        @JsonProperty("notes")
        private Map<String, String> notes;

        public String getBrokerSessionId() {
            return brokerSessionId;
        }

        public void setBrokerSessionId(String brokerSessionId) {
            this.brokerSessionId = brokerSessionId;
        }

        public String getBrokerUserId() {
            return brokerUserId;
        }

        public void setBrokerUserId(String brokerUserId) {
            this.brokerUserId = brokerUserId;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getAuthMethod() {
            return authMethod;
        }

        public void setAuthMethod(String authMethod) {
            this.authMethod = authMethod;
        }

        public boolean isRememberMe() {
            return rememberMe;
        }

        public void setRememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
        }

        public int getStarted() {
            return started;
        }

        public void setStarted(int started) {
            this.started = started;
        }

        public Map<String, String> getNotes() {
            return notes;
        }

        public void setNotes(Map<String, String> notes) {
            this.notes = notes;
        }

    }
}
