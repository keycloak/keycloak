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
import java.util.function.Predicate;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.userprofile.config.UPConfig;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserProfileUtil {

    private static final Logger logger = Logger.getLogger(UserProfileUtil.class);

    public static final String USER_METADATA_GROUP = "user-metadata";

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
     * @param userFederationUsersSelector used to recognize if user belongs to this user-storage provider or not
     * @param guiOrder guiOrder to where to put the attribute
     * @param storageProviderName storageProviderName (just for logging purposes)
     * @return true if attribute was added. False otherwise
     */
    public static boolean addMetadataAttributeToUserProfile(String attrName, UserProfileMetadata metadata, AttributeGroupMetadata metadataGroup, Predicate<AttributeContext> userFederationUsersSelector, int guiOrder, String storageProviderName) {
        // In case that attributes like LDAP_ID, KERBEROS_PRINCIPAL are explicitly defined on user profile, we can prefer defined configuration
        if (!metadata.getAttribute(attrName).isEmpty()) {
            logger.tracef("Ignore adding metadata attribute '%s' to user profile by user storage provider '%s' as attribute is already defined on user profile.", attrName, storageProviderName);
            return false;
        } else {
            logger.tracef("Adding metadata attribute '%s' to user profile by user storage provider '%s' for user profile context '%s'.", attrName, storageProviderName, metadata.getContext().toString());
            Predicate<AttributeContext> onlyAdminCondition = context -> metadata.getContext() == UserProfileContext.USER_API;
            AttributeMetadata attributeMetadata = metadata.addAttribute(attrName, guiOrder, Collections.emptyList())
                    .addWriteCondition(AttributeMetadata.ALWAYS_FALSE)  // Not writable for anyone
                    .addReadCondition(onlyAdminCondition) // Read-only for administrators
                    .setRequired(AttributeMetadata.ALWAYS_FALSE);

            if (metadataGroup != null) {
                attributeMetadata.setAttributeGroupMetadata(metadataGroup);
            }
            attributeMetadata.setSelector(userFederationUsersSelector);
            return true;
        }
    }
}
