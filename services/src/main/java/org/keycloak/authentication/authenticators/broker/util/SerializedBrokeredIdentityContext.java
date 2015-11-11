package org.keycloak.authentication.authenticators.broker.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.requiredactions.util.UpdateProfileContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderDataMarshaller;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SerializedBrokeredIdentityContext implements UpdateProfileContext {

    private String id;
    private String brokerUsername;
    private String modelUsername;
    private String email;
    private String firstName;
    private String lastName;
    private String brokerSessionId;
    private String brokerUserId;
    private String code;
    private String token;

    private String identityProviderId;
    private Map<String, ContextDataEntry> contextData = new HashMap<>();

    @JsonIgnore
    @Override
    public boolean isEditUsernameAllowed() {
        return true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return modelUsername;
    }

    @Override
    public void setUsername(String username) {
        this.modelUsername = username;
    }

    public String getModelUsername() {
        return modelUsername;
    }

    public void setModelUsername(String modelUsername) {
        this.modelUsername = modelUsername;
    }

    public String getBrokerUsername() {
        return brokerUsername;
    }

    public void setBrokerUsername(String modelUsername) {
        this.brokerUsername = modelUsername;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getIdentityProviderId() {
        return identityProviderId;
    }

    public void setIdentityProviderId(String identityProviderId) {
        this.identityProviderId = identityProviderId;
    }

    public Map<String, ContextDataEntry> getContextData() {
        return contextData;
    }

    public void setContextData(Map<String, ContextDataEntry> contextData) {
        this.contextData = contextData;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> result = new HashMap<>();

        for (Map.Entry<String, ContextDataEntry> entry : this.contextData.entrySet()) {
            if (entry.getKey().startsWith("user.attributes.")) {
                ContextDataEntry ctxEntry = entry.getValue();
                String asString = ctxEntry.getData();
                try {
                    List<String> asList = JsonSerialization.readValue(asString, List.class);
                    result.put(entry.getKey().substring(16), asList);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }

        return result;
    }

    @Override
    public void setAttribute(String key, List<String> value) {
        try {
            String listStr = JsonSerialization.writeValueAsString(value);
            ContextDataEntry ctxEntry = ContextDataEntry.create(List.class.getName(), listStr);
            this.contextData.put("user.attributes." + key, ctxEntry);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public List<String> getAttribute(String key) {
        ContextDataEntry ctxEntry = this.contextData.get("user.attributes." + key);
        if (ctxEntry != null) {
            try {
                String asString = ctxEntry.getData();
                List<String> asList = JsonSerialization.readValue(asString, List.class);
                return asList;
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else {
            return null;
        }
    }

    public BrokeredIdentityContext deserialize(KeycloakSession session, ClientSessionModel clientSession) {
        BrokeredIdentityContext ctx = new BrokeredIdentityContext(getId());

        ctx.setUsername(getBrokerUsername());
        ctx.setModelUsername(getModelUsername());
        ctx.setEmail(getEmail());
        ctx.setFirstName(getFirstName());
        ctx.setLastName(getLastName());
        ctx.setBrokerSessionId(getBrokerSessionId());
        ctx.setBrokerUserId(getBrokerUserId());
        ctx.setCode(getCode());
        ctx.setToken(getToken());

        RealmModel realm = clientSession.getRealm();
        IdentityProviderModel idpConfig = realm.getIdentityProviderByAlias(getIdentityProviderId());
        if (idpConfig == null) {
            throw new ModelException("Can't find identity provider with ID " + getIdentityProviderId() + " in realm " + realm.getName());
        }
        IdentityProvider idp = IdentityBrokerService.getIdentityProvider(session, realm, idpConfig.getAlias());
        ctx.setIdpConfig(idpConfig);
        ctx.setIdp(idp);

        IdentityProviderDataMarshaller serializer = idp.getMarshaller();

        for (Map.Entry<String, ContextDataEntry> entry : getContextData().entrySet()) {
            try {
                ContextDataEntry value = entry.getValue();
                Class<?> clazz = Reflections.classForName(value.getClazz(), this.getClass().getClassLoader());

                Object deserialized = serializer.deserialize(value.getData(), clazz);

                ctx.getContextData().put(entry.getKey(), deserialized);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ctx.setClientSession(clientSession);
        return ctx;
    }

    public static SerializedBrokeredIdentityContext serialize(BrokeredIdentityContext context) {
        SerializedBrokeredIdentityContext ctx = new SerializedBrokeredIdentityContext();
        ctx.setId(context.getId());
        ctx.setBrokerUsername(context.getUsername());
        ctx.setModelUsername(context.getModelUsername());
        ctx.setEmail(context.getEmail());
        ctx.setFirstName(context.getFirstName());
        ctx.setLastName(context.getLastName());
        ctx.setBrokerSessionId(context.getBrokerSessionId());
        ctx.setBrokerUserId(context.getBrokerUserId());
        ctx.setCode(context.getCode());
        ctx.setToken(context.getToken());
        ctx.setIdentityProviderId(context.getIdpConfig().getAlias());

        IdentityProviderDataMarshaller serializer = context.getIdp().getMarshaller();

        for (Map.Entry<String, Object> entry : context.getContextData().entrySet()) {
            Object value = entry.getValue();
            String serializedValue = serializer.serialize(value);

            ContextDataEntry ctxEntry = ContextDataEntry.create(value.getClass().getName(), serializedValue);
            ctx.getContextData().put(entry.getKey(), ctxEntry);
        }
        return ctx;
    }

    // Save this context as note to clientSession
    public void saveToClientSession(ClientSessionModel clientSession) {
        try {
            String asString = JsonSerialization.writeValueAsString(this);
            clientSession.setNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE, asString);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static SerializedBrokeredIdentityContext readFromClientSession(ClientSessionModel clientSession) {
        String asString = clientSession.getNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
        if (asString == null) {
            return null;
        } else {
            try {
                return JsonSerialization.readValue(asString, SerializedBrokeredIdentityContext.class);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    public static class ContextDataEntry {

        private String clazz;
        private String data;

        public String getClazz() {
            return clazz;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public static ContextDataEntry create(String clazz, String data) {
            ContextDataEntry entry = new ContextDataEntry();
            entry.setClazz(clazz);
            entry.setData(data);
            return entry;
        }
    }
}
