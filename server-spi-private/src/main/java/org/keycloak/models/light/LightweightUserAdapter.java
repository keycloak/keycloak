/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.light;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.Base64;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.storage.adapter.AbstractInMemoryUserAdapter;
import org.keycloak.util.JsonSerialization;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hmlnarik
 */
@JsonIncludeProperties({
    "id",
    "createdTimestamp",
    "emailVerified",
    "enabled",
    "roleIds",
    "groupIds",
    "attributes",
    "requiredActions",
    "federationLink",
    "serviceAccountClientLink",
    "readonly"
})
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class LightweightUserAdapter extends AbstractInMemoryUserAdapter {

    private Consumer<LightweightUserAdapter> updateHandler = a -> {};

    public static final String ID_PREFIX = "lightweight-";

    public static boolean isLightweightUser(UserModel user) {
        return Profile.isFeatureEnabled(Feature.TRANSIENT_USERS) && user instanceof LightweightUserAdapter;
    }

    public static boolean isLightweightUser(String id) {
        return Profile.isFeatureEnabled(Feature.TRANSIENT_USERS) && id != null && id.startsWith(ID_PREFIX);
    }

    public static String getLightweightUserId(String id) {
        try {
            return id == null || id.length() < ID_PREFIX.length()
              ? null
              : new String(Base64.decode(id.substring(ID_PREFIX.length())), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    public LightweightUserAdapter(KeycloakSession session, String id) {
        super(session, null, ID_PREFIX + Base64.encodeBytes(id.getBytes(StandardCharsets.UTF_8)));
    }

    protected LightweightUserAdapter() {
    }

    public static LightweightUserAdapter fromString(KeycloakSession session, RealmModel realm, String serializedForm) {
        if (serializedForm == null) {
            return null;
        }
        try {
            LightweightUserAdapter res = JsonSerialization.readValue(serializedForm, LightweightUserAdapter.class);
            res.session = session;
            res.realm = realm;
            return res;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        return EmptyCredentialManager.INSTANCE;
    }

    public String serialize() {
        try {
            return JsonSerialization.writeValueAsString(this);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        super.deleteRoleMapping(role); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void grantRole(RoleModel role) {
        super.grantRole(role); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        super.setServiceAccountClientLink(clientInternalId); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void setFederationLink(String link) {
        super.setFederationLink(link); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void leaveGroup(GroupModel group) {
        super.leaveGroup(group); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void joinGroup(GroupModel group) {
        super.joinGroup(group); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        super.setEmailVerified(verified); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        super.removeRequiredAction(action); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        super.addRequiredAction(action); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void removeRequiredAction(String action) {
        super.removeRequiredAction(action); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void addRequiredAction(String action) {
        super.addRequiredAction(action); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void removeAttribute(String name) {
        super.removeAttribute(name); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        super.setAttribute(name, values); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        super.setSingleAttribute(name, value); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        super.setCreatedTimestamp(timestamp); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void setReadonly(boolean flag) {
        super.setReadonly(flag); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void addDefaults() {
        super.addDefaults(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    @Override
    public void setUsername(String username) {
        super.setUsername(username); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        update();
    }

    public void setUpdateHandler(Consumer<LightweightUserAdapter> updateHandler) {
        this.updateHandler = updateHandler == null ? lua -> {} : updateHandler;
    }

    private void update() {
        updateHandler.accept(this);
    }

}
