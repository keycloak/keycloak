/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ClientScopeDecorator implements ClientScopeModel {

    private final ClientScopeModel delegate;
    private final String name;

    public ClientScopeDecorator(ClientScopeModel delegate, String name) {
        this.delegate = delegate;
        this.name = name;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RealmModel getRealm() {
        return delegate.getRealm();
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    @Override
    public String getProtocol() {
        return delegate.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        delegate.setProtocol(protocol);
    }

    @Override
    public void setAttribute(String name, String value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public boolean isDisplayOnConsentScreen() {
        return delegate.isDisplayOnConsentScreen();
    }

    @Override
    public void setDisplayOnConsentScreen(boolean displayOnConsentScreen) {
        delegate.setDisplayOnConsentScreen(displayOnConsentScreen);
    }

    @Override
    public String getConsentScreenText() {
        return delegate.getConsentScreenText();
    }

    @Override
    public void setConsentScreenText(String consentScreenText) {
        delegate.setConsentScreenText(consentScreenText);
    }

    @Override
    public String getGuiOrder() {
        return delegate.getGuiOrder();
    }

    @Override
    public void setGuiOrder(String guiOrder) {
        delegate.setGuiOrder(guiOrder);
    }

    @Override
    public boolean isIncludeInTokenScope() {
        return delegate.isIncludeInTokenScope();
    }

    @Override
    public void setIncludeInTokenScope(boolean includeInTokenScope) {
        delegate.setIncludeInTokenScope(includeInTokenScope);
    }

    @Override
    public boolean isDynamicScope() {
        return delegate.isDynamicScope();
    }

    @Override
    public void setIsDynamicScope(boolean isDynamicScope) {
        delegate.setIsDynamicScope(isDynamicScope);
    }

    @Override
    public String getDynamicScopeRegexp() {
        return delegate.getDynamicScopeRegexp();
    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        return delegate.getProtocolMappersStream();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        return delegate.addProtocolMapper(model);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        delegate.removeProtocolMapper(mapping);
    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        delegate.updateProtocolMapper(mapping);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return delegate.getProtocolMapperById(id);
    }

    @Override
    public List<ProtocolMapperModel> getProtocolMapperByType(String type) {
        return delegate.getProtocolMapperByType(type);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        return delegate.getProtocolMapperByName(protocol, name);
    }

    @Override
    public Stream<RoleModel> getScopeMappingsStream() {
        return delegate.getScopeMappingsStream();
    }

    @Override
    public Stream<RoleModel> getRealmScopeMappingsStream() {
        return delegate.getRealmScopeMappingsStream();
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        delegate.addScopeMapping(role);
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        delegate.deleteScopeMapping(role);
    }

    @Override
    public boolean hasDirectScope(RoleModel role) {
        return delegate.hasDirectScope(role);
    }

    @Override
    public boolean hasScope(RoleModel role) {
        return delegate.hasScope(role);
    }
}
