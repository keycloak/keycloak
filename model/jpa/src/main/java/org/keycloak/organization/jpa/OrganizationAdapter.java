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

package org.keycloak.organization.jpa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.jpa.entities.OrganizationDomainEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.utils.EmailValidationUtil;
import org.keycloak.utils.StringUtil;

import static java.util.Optional.ofNullable;

public final class OrganizationAdapter implements OrganizationModel, JpaModel<OrganizationEntity> {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationEntity entity;
    private final OrganizationProvider provider;
    private GroupModel group;
    private Map<String, List<String>> attributes;

    public OrganizationAdapter(KeycloakSession session, RealmModel realm, OrganizationEntity entity, OrganizationProvider provider) {
        this.session = session;
        this.realm = realm;
        this.entity = entity;
        this.provider = provider;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    RealmModel getRealm() {
        return realm;
    }

    public String getGroupId() {
        return entity.getGroupId();
    }

    void setGroupId(String id) {
        entity.setGroupId(id);
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public String getAlias() {
        return entity.getAlias();
    }

    @Override
    public void setAlias(String alias) {
        if (StringUtil.isBlank(alias)) {
            alias = getName();
        }
        if (alias.equals(entity.getAlias())) {
            return;
        }
        if (StringUtil.isNotBlank(entity.getAlias())) {
            throw new ModelValidationException("Cannot change the alias");
        }
        entity.setAlias(alias);
    }

    @Override
    public boolean isEnabled() {
        return provider.isEnabled() && entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        entity.setDescription(description);
    }

    @Override
    public String getRedirectUrl() {
        return entity.getRedirectUrl();
    }

    @Override
    public void setRedirectUrl(String redirectUrl) {
        entity.setRedirectUrl(redirectUrl);
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        if (attributes == null) {
            return;
        }

        // add organization to the session as the following code updates the underlying group
        OrganizationModel current = session.getContext().getOrganization();
        if (current == null) {
            session.getContext().setOrganization(this);
        }

        try {
            Set<String> attrsToRemove = getAttributes().keySet();
            attrsToRemove.removeAll(attributes.keySet());
            attrsToRemove.forEach(group::removeAttribute);
            attributes.forEach(group::setAttribute);
        } finally {
            if (current == null) {
                session.getContext().setOrganization(null);
            }
        }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (attributes == null) {
            attributes = ofNullable(getGroup().getAttributes()).orElse(Map.of());
        }
        return attributes;
    }

    @Override
    public Stream<OrganizationDomainModel> getDomains() {
        return entity.getDomains().stream().map(this::toModel);
    }

    @Override
    public void setDomains(Set<OrganizationDomainModel> domains) {
        if (domains == null) {
            return;
        }

        Map<String, OrganizationDomainModel> modelMap = domains.stream()
                .map(this::validateDomain)
                .collect(Collectors.toMap(OrganizationDomainModel::getName, Function.identity()));

        for (OrganizationDomainEntity domainEntity : new HashSet<>(this.entity.getDomains())) {
            // update the existing domain (verified flag and exclusions can be changed).
            if (modelMap.containsKey(domainEntity.getName())) {
                OrganizationDomainModel model = modelMap.get(domainEntity.getName());
                domainEntity.setVerified(model.isVerified());
                domainEntity.setExcludedSubdomains(serializeExclusions(model.getExcludedSubdomains()));
                modelMap.remove(domainEntity.getName());
            } else {
                // remove domain that is not found in the new set.
                this.entity.removeDomain(domainEntity);
                // check if any idp is assigned to the removed domain, and unset the domain if that's the case.
                getIdentityProviders()
                        .filter(idp -> Objects.equals(domainEntity.getName(), idp.getConfig().get(ORGANIZATION_DOMAIN_ATTRIBUTE)))
                        .forEach(idp -> {
                            idp.getConfig().remove(ORGANIZATION_DOMAIN_ATTRIBUTE);
                            session.identityProviders().update(idp);
                        });
            }
        }

        // create the remaining domains.
        for (OrganizationDomainModel model : modelMap.values()) {
            OrganizationDomainEntity domainEntity = new OrganizationDomainEntity();
            domainEntity.setId(KeycloakModelUtils.generateId());
            domainEntity.setName(model.getName());
            domainEntity.setVerified(model.isVerified());
            domainEntity.setExcludedSubdomains(serializeExclusions(model.getExcludedSubdomains()));
            domainEntity.setOrganization(this.entity);
            this.entity.addDomain(domainEntity);
        }
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProviders() {
        return provider.getIdentityProviders(this);
    }

    @Override
    public boolean isManaged(UserModel user) {
        return provider.isManagedMember(this, user);
    }

    @Override
    public boolean isMember(UserModel user) {
        return provider.isMember(this, user);
    }

    @Override
    public OrganizationEntity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("id=")
                .append(getId())
                .append(",")
                .append("name=")
                .append(getName())
                .append(",")
                .append("realm=")
                .append(getRealm().getName())
                .append(",")
                .append("groupId=")
                .append(getGroupId()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationModel)) return false;

        OrganizationModel that = (OrganizationModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    private OrganizationDomainModel toModel(OrganizationDomainEntity entity) {
        return new OrganizationDomainModel(entity.getName(), entity.isVerified(), 
                deserializeExclusions(entity.getExcludedSubdomains()));
    }
    
    /**
     * Serialize a set of excluded subdomains to a comma-separated string.
     */
    private String serializeExclusions(Set<String> exclusions) {
        if (exclusions == null || exclusions.isEmpty()) {
            return null;
        }
        return String.join(",", exclusions);
    }
    
    /**
     * Deserialize a comma-separated string of excluded subdomains to a set.
     */
    private Set<String> deserializeExclusions(String exclusions) {
        if (exclusions == null || exclusions.trim().isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(exclusions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Validates the domain. Specifically, the method first checks if the specified domain is valid,
     * and then checks if the domain is not already linked to a different organization.
     *
     * @param domainModel the {@link OrganizationDomainModel} representing the domain being added.
     * @throws {@link ModelValidationException} if the domain is invalid or is already linked to a different organization.
     */
    private OrganizationDomainModel validateDomain(OrganizationDomainModel domainModel) {
        String domainName = domainModel.getName();
        
        if (StringUtil.isBlank(domainName)) {
            throw new ModelValidationException("Domain name cannot be empty");
        }
        
        String baseDomain = domainName;
        boolean isWildcard = false;
        
        // Handle wildcard pattern (*.domain)
        if (domainName.startsWith("*.")) {
            if (domainName.length() <= 2) {
                throw new ModelValidationException("Wildcard domain must specify a base domain: " + domainName);
            }
            baseDomain = domainName.substring(2);
            isWildcard = true;
            
            // Check for invalid double wildcards
            if (baseDomain.contains("*")) {
                throw new ModelValidationException("Multiple wildcards are not allowed: " + domainName);
            }
        }
        
        // Validate the base domain format
        if (!EmailValidationUtil.isValidEmail("user@" + baseDomain)) {
            throw new ModelValidationException("The specified domain is invalid: " + domainName);
        }
        
        // Validate exclusions if present
        if (domainModel.getExcludedSubdomains() != null && !domainModel.getExcludedSubdomains().isEmpty()) {
            if (!isWildcard) {
                throw new ModelValidationException("Excluded subdomains can only be specified for wildcard domains");
            }
            
            for (String exclusion : domainModel.getExcludedSubdomains()) {
                if (StringUtil.isBlank(exclusion)) {
                    throw new ModelValidationException("Excluded subdomain cannot be empty");
                }
                // Validate that exclusion is a valid domain format
                if (!EmailValidationUtil.isValidEmail("user@" + exclusion)) {
                    throw new ModelValidationException("Invalid excluded subdomain: " + exclusion);
                }
                // Validate that exclusion is actually a subdomain of the base domain
                if (!exclusion.toLowerCase().equals(baseDomain.toLowerCase()) && 
                    !exclusion.toLowerCase().endsWith("." + baseDomain.toLowerCase())) {
                    throw new ModelValidationException("Excluded subdomain '" + exclusion + "' must be a subdomain of '" + baseDomain + "'");
                }
            }
        }
        
        // Check for conflicts with other organizations
        OrganizationModel orgModel = provider.getByDomainName(baseDomain);
        if (orgModel != null && !Objects.equals(getId(), orgModel.getId())) {
            throw new ModelValidationException("Domain " + domainName + " is already linked to organization " + orgModel.getName() + " in realm " + realm.getName());
        }
        
        return domainModel;
    }
    
    /**
     * Validates that all exclusion domains have a corresponding wildcard domain.
     * Exclusions (-.subdomain.domain) require a matching wildcard (*.domain or *.parent.domain).
     */
    private void validateExclusionsHaveWildcards(Set<String> domainNames) {
        List<String> exclusions = domainNames.stream()
                .filter(name -> name.startsWith(\"-.\"))
                .collect(Collectors.toList());
        
        List<String> wildcards = domainNames.stream()
                .filter(name -> name.startsWith(\"*.\"))
                .map(name -> name.substring(2)) // Strip *.
                .collect(Collectors.toList());
        
        for (String exclusion : exclusions) {
            String excludedDomain = exclusion.substring(2); // Strip -.
            
            // Check if this exclusion matches any wildcard
            boolean hasMatchingWildcard = wildcards.stream()
                    .anyMatch(wildcard -> 
                        excludedDomain.equals(wildcard) || excludedDomain.endsWith(\".\" + wildcard)
                    );
            
            if (!hasMatchingWildcard) {
                throw new ModelValidationException(\"Exclusion \" + exclusion + \" requires a matching wildcard domain (e.g., *.\" + extractBaseDomain(excludedDomain) + \")\");\n            }\n        }\n    }\n    \n    /**\n     * Extract base domain from a subdomain. For example:\n     * admin.example.com -> example.com\n     * hr.dept.example.com -> dept.example.com or example.com (returns closest parent)\n     */\n    private String extractBaseDomain(String domain) {\n        int firstDot = domain.indexOf('.');\n        if (firstDot > 0 && firstDot < domain.length() - 1) {\n            return domain.substring(firstDot + 1);\n        }\n        return domain;\n    }

    private GroupModel getGroup() {
        if (group == null) {
            group = realm.getGroupById(getGroupId());
        }
        return group;
    }
}
