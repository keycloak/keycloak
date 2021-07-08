/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.userprofile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class AttributeMetadata {

    public static final Predicate<AttributeContext> ALWAYS_TRUE = context -> true;
    public static final Predicate<AttributeContext> ALWAYS_FALSE = context -> false;

    private final String attributeName;
    private String attributeDisplayName;
    private final Predicate<AttributeContext> selector;
    private final Predicate<AttributeContext> writeAllowed;
    /** Predicate to decide if attribute is required, it is handled as required if predicate is null */
    private final Predicate<AttributeContext> required;
    private final Predicate<AttributeContext> readAllowed;
    private List<AttributeValidatorMetadata> validators;
    private Map<String, Object> annotations;
    private int guiOrder;
    

    AttributeMetadata(String attributeName, int guiOrder) {
        this(attributeName, guiOrder, ALWAYS_TRUE, ALWAYS_TRUE, ALWAYS_TRUE, ALWAYS_TRUE);
    }

    AttributeMetadata(String attributeName, int guiOrder, Predicate<AttributeContext> writeAllowed, Predicate<AttributeContext> required) {
        this(attributeName, guiOrder, ALWAYS_TRUE, writeAllowed, required, ALWAYS_TRUE);
    }

    AttributeMetadata(String attributeName, int guiOrder, Predicate<AttributeContext> selector) {
        this(attributeName, guiOrder, selector, ALWAYS_FALSE, ALWAYS_TRUE, ALWAYS_TRUE);
    }

    AttributeMetadata(String attributeName, int guiOrder, List<String> scopes, Predicate<AttributeContext> writeAllowed, Predicate<AttributeContext> required) {
        this(attributeName, guiOrder, context -> {
            KeycloakSession session = context.getSession();
            AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();

            if (authSession == null) {
                return false;
            }

            ClientScopeProvider clientScopes = session.clientScopes();
            RealmModel realm = session.getContext().getRealm();

            // TODO UserProfile - LOOKS LIKE THIS DOESN'T WORK FOR SOME AUTH FLOWS, LIKE
            // REGISTER?
            if (authSession.getClientScopes().stream().anyMatch(scopes::contains)) {
                return true;
            }

            return authSession.getClientScopes().stream()
                    .map(id -> clientScopes.getClientScopeById(realm, id).getName()).anyMatch(scopes::contains);
        }, writeAllowed, required, ALWAYS_TRUE);
    }

    AttributeMetadata(String attributeName, int guiOrder, Predicate<AttributeContext> selector, Predicate<AttributeContext> writeAllowed,
            Predicate<AttributeContext> required,
            Predicate<AttributeContext> readAllowed) {
        this.attributeName = attributeName;
        this.selector = selector;
        this.writeAllowed = writeAllowed;
        this.required = required;
        this.readAllowed = readAllowed;
        this.guiOrder = guiOrder;
    }

    public String getName() {
        return attributeName;
    }

    public int getGuiOrder() {
        return guiOrder;
    }

    public AttributeMetadata setGuiOrder(int guiOrder) {
        this.guiOrder = guiOrder;
        return this;
    }

    public boolean isSelected(AttributeContext context) {
        return selector.test(context);
    }

    public boolean isReadOnly(AttributeContext context) {
        return !writeAllowed.test(context);
    }

    public boolean canView(AttributeContext context) {
        return readAllowed.test(context);
    }

    public boolean canEdit(AttributeContext context) {
        return writeAllowed.test(context);
    }

    /**
     * Check if attribute is required based on it's predicate, it is handled as required if predicate is null
     * @param context to evaluate requirement of the attribute from
     * @return true if attribute is required in provided context
     */
    public boolean isRequired(AttributeContext context) {
        return required == null || required.test(context);
    }

    public List<AttributeValidatorMetadata> getValidators() {
        return validators;
    }

    public AttributeMetadata addValidator(List<AttributeValidatorMetadata> validators) {
        if (this.validators == null) {
            this.validators = new ArrayList<>();
        }

        this.validators.addAll(validators.stream().filter(Objects::nonNull).collect(Collectors.toList()));

        return this;
    }

    public AttributeMetadata addValidator(AttributeValidatorMetadata validator) {
        addValidator(Arrays.asList(validator));
        return this;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    public AttributeMetadata addAnnotations(Map<String, Object> annotations) {
        if(annotations != null) {
            if(this.annotations == null) {
                this.annotations = new HashMap<>();
            }

            this.annotations.putAll(annotations);
        }
        return this;
    }

    @Override
    public AttributeMetadata clone() {
        AttributeMetadata cloned = new AttributeMetadata(attributeName, guiOrder, selector, writeAllowed, required, readAllowed);
        // we clone validators list to allow adding or removing validators. Validators
        // itself are not cloned as we do not expect them to be reconfigured.
        if (validators != null) {
            cloned.addValidator(validators);
        }
        //we clone annotations map to allow adding to or removing from it
        if(annotations != null) {
            cloned.addAnnotations(annotations);
        }
        cloned.setAttributeDisplayName(attributeDisplayName);
        return cloned;
    }
    
    public String getAttributeDisplayName() {
        if(attributeDisplayName == null || attributeDisplayName.trim().isEmpty())
            return attributeName;
        return attributeDisplayName;
    }

    public AttributeMetadata setAttributeDisplayName(String attributeDisplayName) {
        if(attributeDisplayName != null)
            this.attributeDisplayName = attributeDisplayName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AttributeMetadata)) return false;

        AttributeMetadata that = (AttributeMetadata) o;

        return that.getName().equals(getName());
    }

    @Override
    public int hashCode() {
        return attributeName.hashCode();
    }
}
