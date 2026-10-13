/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.userprofile;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.representations.idm.UserProfileAttributeGroupMetadata;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.validate.Validators;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserProfileUtil {

    private static final Logger logger = Logger.getLogger(UserProfileUtil.class);

    public static final String USER_METADATA_GROUP = "user-metadata";

    public static final Predicate<AttributeContext> ONLY_ADMIN_CONDITION = context -> context.getContext().isAdminContext();

    /**
     * Find the metadata group "user-metadata"
     *
     * @param session
     * @return metadata group if exists, otherwise null
     */
    public static AttributeGroupMetadata lookupUserMetadataGroup(KeycloakSession session) {
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        UPConfig config = provider.getConfiguration();
        return config.getGroups().stream()
                .filter(upGroup -> USER_METADATA_GROUP.equals(upGroup.getName()))
                .map(upGroup -> new AttributeGroupMetadata(upGroup.getName(), upGroup.getDisplayHeader(), upGroup.getDisplayDescription(), upGroup.getAnnotations()))
                .findAny()
                .orElse(null);
    }

    /**
     * Adds metadata attribute to the user-profile for users from specified userStorageProvider
     *
     * @param attrName attribute name
     * @param metadata user-profile metadata where attribute would be added
     * @param metadataGroup metadata group in user-profile
     * @param guiOrder guiOrder to where to put the attribute
     * @param storageProviderName storageProviderName (just for logging purposes)
     * @return the attribute metadata if attribute was created. False otherwise
     */
    public static AttributeMetadata createAttributeMetadata(String attrName, UserProfileMetadata metadata, AttributeGroupMetadata metadataGroup, int guiOrder, String storageProviderName) {
        return createAttributeMetadata(attrName, metadata, metadataGroup, ONLY_ADMIN_CONDITION, AttributeMetadata.ALWAYS_FALSE, guiOrder, storageProviderName);
    }

    public static AttributeMetadata createAttributeMetadata(String attrName, UserProfileMetadata metadata, int guiOrder, String storageProviderName) {
        return createAttributeMetadata(attrName, metadata, null, ONLY_ADMIN_CONDITION, ONLY_ADMIN_CONDITION, guiOrder, storageProviderName);
    }

    private static AttributeMetadata createAttributeMetadata(String attrName, UserProfileMetadata metadata, AttributeGroupMetadata metadataGroup, Predicate<AttributeContext> readCondition, Predicate<AttributeContext> writeCondition, int guiOrder, String storageProviderName) {
        if (!metadata.getAttribute(attrName).isEmpty()) {
            logger.tracef("Ignore adding metadata attribute '%s' to user profile by user storage provider '%s' as attribute is already defined on user profile.", attrName, storageProviderName);
        } else {
            logger.tracef("Adding metadata attribute '%s' to user profile by user storage provider '%s' for user profile context '%s'.", attrName, storageProviderName, metadata.getContext().toString());

            AttributeMetadata attributeMetadata = new AttributeMetadata(attrName, guiOrder)
                    .setValidators(Collections.emptyList())
                    .addWriteCondition(writeCondition)
                    .addReadCondition(readCondition)
                    .setRequired(AttributeMetadata.ALWAYS_FALSE);

            if (metadataGroup != null) {
                attributeMetadata.setAttributeGroupMetadata(metadataGroup);
            }

            return attributeMetadata;
        }

        return null;
    }

    /**
     * Returns whether the attribute with the given {@code name} is a root attribute.
     *
     * @param name the attribute name
     * @return
     */
    public static boolean isRootAttribute(String name) {
        return UserModel.USERNAME.equals(name)
                || UserModel.EMAIL.equals(name)
                || UserModel.FIRST_NAME.equals(name)
                || UserModel.LAST_NAME.equals(name)
                || UserModel.LOCALE.equals(name);
    }

    public static org.keycloak.representations.idm.UserProfileMetadata createUserProfileMetadata(KeycloakSession session, UserProfile profile) {
        Attributes profileAttributes = profile.getAttributes();
        Map<String, List<String>> am = profileAttributes.getReadable();

        if(am == null)
            return null;
        Map<String, List<String>> unmanagedAttributes = profileAttributes.getUnmanagedAttributes();

        List<UserProfileAttributeMetadata> attributes = am.keySet().stream()
                .map(profileAttributes::getMetadata)
                .filter(Objects::nonNull)
                .filter(attributeMetadata -> !unmanagedAttributes.containsKey(attributeMetadata.getName()))
                .sorted(Comparator.comparingInt(AttributeMetadata::getGuiOrder))
                .map(sam -> toRestMetadata(sam, session, profile))
                .collect(Collectors.toList());

        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        UPConfig config = provider.getConfiguration();

        List<UserProfileAttributeGroupMetadata> groups = config.getGroups().stream().map(new Function<UPGroup, UserProfileAttributeGroupMetadata>() {
            @Override
            public UserProfileAttributeGroupMetadata apply(UPGroup upGroup) {
                return new UserProfileAttributeGroupMetadata(upGroup.getName(), upGroup.getDisplayHeader(), upGroup.getDisplayDescription(), upGroup.getAnnotations());
            }
        }).collect(Collectors.toList());

        return new org.keycloak.representations.idm.UserProfileMetadata(attributes, groups);
    }

    private static UserProfileAttributeMetadata toRestMetadata(AttributeMetadata am, KeycloakSession session, UserProfile profile) {
        String group = null;

        if (am.getAttributeGroupMetadata() != null) {
            group = am.getAttributeGroupMetadata().getName();
        }

        Attributes attributes = profile.getAttributes();

        return new UserProfileAttributeMetadata(am.getName(),
                am.getAttributeDisplayName(),
                attributes.isRequired(am.getName()),
                attributes.isReadOnly(am.getName()),
                group,
                attributes.getAnnotations(am.getName()),
                toValidatorMetadata(am, session),
                am.isMultivalued(),
                am.getDefaultValue());
    }

    private static Map<String, Map<String, Object>> toValidatorMetadata(AttributeMetadata am, KeycloakSession session){
        // we return only validators which are instance of ConfiguredProvider. Others are expected as internal.
        return am.getValidators() == null ? null : am.getValidators().stream()
                .filter(avm -> (Validators.validator(session, avm.getValidatorId()) instanceof ConfiguredProvider))
                .collect(Collectors.toMap(AttributeValidatorMetadata::getValidatorId, AttributeValidatorMetadata::getValidatorConfig));
    }
}
