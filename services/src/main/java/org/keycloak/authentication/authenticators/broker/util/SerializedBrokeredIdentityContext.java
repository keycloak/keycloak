/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.authenticators.broker.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.keycloak.authentication.requiredactions.util.UpdateProfileContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderDataMarshaller;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SerializedBrokeredIdentityContext implements UpdateProfileContext {

    private String id;
    private String brokerUsername;
    private String brokerSessionId;
    private String brokerUserId;
    private String code;
    private String token;

    @JsonIgnore
    private boolean emailAsUsername;

    private String identityProviderId;
    private Map<String, ContextDataEntry> contextData = new HashMap<>();

    @JsonIgnore
    @Override
    public boolean isEditUsernameAllowed() {
        return !emailAsUsername;
    }

    @JsonIgnore
    @Override
    public UserProfileContext getUserProfileContext() {
        return UserProfileContext.IDP_REVIEW;
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
        return getFirstAttribute(UserModel.USERNAME);
    }

    @Override
    public void setUsername(String username) {
        setSingleAttribute(UserModel.USERNAME, username);
    }

    @JsonIgnore
    @Override
    public boolean isEditEmailAllowed() {
        return true;
    }

    public String getModelUsername() {
        return getFirstAttribute(UserModel.USERNAME);
    }

    public void setModelUsername(String modelUsername) {
        setSingleAttribute(UserModel.USERNAME, modelUsername);
    }

    public String getBrokerUsername() {
        return brokerUsername;
    }

    public void setBrokerUsername(String modelUsername) {
        this.brokerUsername = modelUsername;
    }

    @Override
    public String getEmail() {
        return getFirstAttribute(UserModel.EMAIL);
    }

    @Override
    public void setEmail(String email) {
        setSingleAttribute(UserModel.EMAIL, email);
    }

    @Override
    public String getFirstName() {
        return getFirstAttribute(UserModel.FIRST_NAME);
    }

    @Override
    public void setFirstName(String firstName) {
        setSingleAttribute(UserModel.FIRST_NAME, firstName);
    }

    @Override
    public String getLastName() {
        return getFirstAttribute(UserModel.LAST_NAME);
    }

    @Override
    public void setLastName(String lastName) {
        setSingleAttribute(UserModel.LAST_NAME, lastName);
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

    @JsonIgnore
    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> result = new HashMap<>();

        for (Map.Entry<String, ContextDataEntry> entry : this.contextData.entrySet()) {
            if (entry.getKey().startsWith(Constants.USER_ATTRIBUTES_PREFIX)) {
                String attrName = entry.getKey().substring(16); // length of USER_ATTRIBUTES_PREFIX
                List<String> asList = getAttribute(attrName);
                result.put(attrName, asList);
            }
        }

        return result;
    }

    @JsonIgnore
    @Override
    public void setSingleAttribute(String name, String value) {
        List<String> list = new ArrayList<>();
        list.add(value);
        setAttribute(name, list);
    }

    @JsonIgnore
    @Override
    public void setAttribute(String key, List<String> value) {
        try {
            byte[] listBytes = JsonSerialization.writeValueAsBytes(value);
            String listStr = Base64Url.encode(listBytes);
            ContextDataEntry ctxEntry = ContextDataEntry.create(List.class.getName(), listStr);
            this.contextData.put(Constants.USER_ATTRIBUTES_PREFIX + key, ctxEntry);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @JsonIgnore
    @Override
    public List<String> getAttribute(String key) {
        return this.getAttributeStream(key).collect(Collectors.toList());
    }

    @JsonIgnore
    @Override
    public Stream<String> getAttributeStream(String key) {
        ContextDataEntry ctxEntry = this.contextData.get(Constants.USER_ATTRIBUTES_PREFIX + key);
        if (ctxEntry != null) {
            try {
                String asString = ctxEntry.getData();
                byte[] asBytes = Base64Url.decode(asString);
                return JsonSerialization.readValue(asBytes, List.class).stream();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else {
            return Stream.empty();
        }
    }

    @JsonIgnore
    @Override
    public String getFirstAttribute(String name) {
        List<String> attrs = getAttribute(name);
        if (attrs == null || attrs.isEmpty()) {
            return null;
        } else {
            return attrs.get(0);
        }
    }

    public BrokeredIdentityContext deserialize(KeycloakSession session, AuthenticationSessionModel authSession) {
        RealmModel realm = authSession.getRealm();
        IdentityProviderModel idpConfig = session.identityProviders().getByAlias(getIdentityProviderId());

        if (idpConfig == null) {
            throw new ModelException("Can't find identity provider with ID " + getIdentityProviderId() + " in realm " + realm.getName());
        }

        BrokeredIdentityContext ctx = new BrokeredIdentityContext(getId(), idpConfig);

        ctx.setUsername(getBrokerUsername());
        ctx.setModelUsername(getModelUsername());
        ctx.setEmail(getEmail());
        ctx.setFirstName(getFirstName());
        ctx.setLastName(getLastName());
        ctx.setBrokerSessionId(getBrokerSessionId());
        ctx.setBrokerUserId(getBrokerUserId());
        ctx.setToken(getToken());

        UserAuthenticationIdentityProvider<?> idp = IdentityBrokerService.getIdentityProvider(session, idpConfig.getAlias());
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

        ctx.setAuthenticationSession(authSession);
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
        ctx.setToken(context.getToken());
        ctx.setIdentityProviderId(context.getIdpConfig().getAlias());

        ctx.emailAsUsername = context.getAuthenticationSession().getRealm().isRegistrationEmailAsUsername();

        IdentityProviderDataMarshaller serializer = context.getIdp().getMarshaller();

        for (Map.Entry<String, Object> entry : context.getContextData().entrySet()) {
            Object value = entry.getValue();
            String serializedValue = serializer.serialize(value);

            ContextDataEntry ctxEntry = ContextDataEntry.create(value.getClass().getName(), serializedValue);
            ctx.getContextData().put(entry.getKey(), ctxEntry);
        }
        return ctx;
    }

    // Save this context as note to authSession
    public void saveToAuthenticationSession(AuthenticationSessionModel authSession, String noteKey) {
        try {
            String asString = JsonSerialization.writeValueAsString(this);
            authSession.setAuthNote(noteKey, asString);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static SerializedBrokeredIdentityContext readFromAuthenticationSession(AuthenticationSessionModel authSession, String noteKey) {
        String asString = authSession.getAuthNote(noteKey);
        if (asString == null) {
            return null;
        } else {
            try {
                SerializedBrokeredIdentityContext serializedCtx = JsonSerialization.readValue(asString, SerializedBrokeredIdentityContext.class);
                serializedCtx.emailAsUsername = authSession.getRealm().isRegistrationEmailAsUsername();
                return serializedCtx;
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
