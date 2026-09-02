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
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.keycloak.userprofile.AttributeMetadata.ALWAYS_TRUE;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class UserProfileMetadata implements Cloneable {

    private final UserProfileContext context;
    private List<AttributeMetadata> attributes;

    public UserProfileMetadata(UserProfileContext context) {
        this.context = context;
    }

    public List<AttributeMetadata> getAttributes() {
        return attributes;
    }

    public void addAttributes(List<AttributeMetadata> metadata) {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        attributes.addAll(metadata);
    }

    public AttributeMetadata addAttribute(AttributeMetadata metadata) {
        addAttributes(Arrays.asList(metadata));
        return metadata;
    }
    
    public AttributeMetadata addAttribute(String name, int guiOrder, AttributeValidatorMetadata... validator) {
        return addAttribute(name, guiOrder, Arrays.asList(validator));
    }

    public AttributeMetadata addAttribute(String name, int guiOrder, Predicate<AttributeContext> writeAllowed, Predicate<AttributeContext> readAllowed, AttributeValidatorMetadata... validator) {
        return addAttribute(new AttributeMetadata(name, guiOrder, ALWAYS_TRUE, writeAllowed, ALWAYS_TRUE, readAllowed).addValidators(Arrays.asList(validator)));
    }

    public AttributeMetadata addAttribute(String name, int guiOrder, Predicate<AttributeContext> writeAllowed, List<AttributeValidatorMetadata> validators) {
        return addAttribute(new AttributeMetadata(name, guiOrder, ALWAYS_TRUE, writeAllowed, ALWAYS_TRUE, ALWAYS_TRUE).addValidators(validators));
    }

    public AttributeMetadata addAttribute(String name, int guiOrder, Predicate<AttributeContext> writeAllowed, Predicate<AttributeContext> required, List<AttributeValidatorMetadata> validators) {
        return addAttribute(new AttributeMetadata(name, guiOrder, ALWAYS_TRUE, writeAllowed, required, ALWAYS_TRUE).addValidators(validators));
    }

    public AttributeMetadata addAttribute(String name, int guiOrder, List<AttributeValidatorMetadata> validators) {
        return addAttribute(new AttributeMetadata(name, guiOrder).addValidators(validators));
    }

    public AttributeMetadata addAttribute(String name, int guiOrder, List<AttributeValidatorMetadata> validator, Predicate<AttributeContext> selector, Predicate<AttributeContext> writeAllowed, Predicate<AttributeContext> required, Predicate<AttributeContext> readAllowed) {
        return addAttribute(new AttributeMetadata(name, guiOrder, selector, writeAllowed, required, readAllowed).addValidators(validator));
    }

    /**
     * Get existing AttributeMetadata for attribute of given name.
     * 
     * @param name of the attribute
     * @return list of existing metadata for given attribute, never null
     */
    public List<AttributeMetadata> getAttribute(String name) {
        if (attributes == null)
            return Collections.emptyList();
        return attributes.stream().filter((c) -> name.equals(c.getName())).collect(Collectors.toList());

    }

    public UserProfileContext getContext() {
        return context;
    }

    @Override
    public UserProfileMetadata clone() {
        UserProfileMetadata metadata = new UserProfileMetadata(this.context);

        //deeply clone AttributeMetadata so we can modify them (add validators etc) 
        if (attributes != null) {
            metadata.addAttributes(attributes.stream().map(AttributeMetadata::clone).collect(Collectors.toList()));
        }

        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserProfileMetadata)) return false;

        UserProfileMetadata that = (UserProfileMetadata) o;
        return that.getContext().equals(getContext());
    }

    @Override
    public int hashCode() {
        return getContext().hashCode();
    }
}
