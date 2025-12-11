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

package org.keycloak.storage.ldap.mappers;

import java.util.Comparator;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPConfig;

/**
 * TODO: Possibly add "priority" instead of hardcoding behaviour
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPMappersComparator {

    private LDAPConfig ldapConfig;

    public LDAPMappersComparator(LDAPConfig ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public Comparator<ComponentModel> sortAsc() {
        return new ImportantFirstComparator(ldapConfig);
    }

    public Comparator<ComponentModel> sortDesc() {
        return new ImportantFirstComparator(ldapConfig).reversed();
    }


    private static class ImportantFirstComparator implements Comparator<ComponentModel> {

        private final LDAPConfig ldapConfig;

        public ImportantFirstComparator(LDAPConfig ldapConfig) {
            this.ldapConfig = ldapConfig;
        }

        @Override
        public int compare(ComponentModel o1, ComponentModel o2) {
            // UserAttributeLDAPFederationMapper first
            boolean isO1AttrMapper = o1.getProviderId().equals(UserAttributeLDAPStorageMapperFactory.PROVIDER_ID);
            boolean isO2AttrMapper = o2.getProviderId().equals(UserAttributeLDAPStorageMapperFactory.PROVIDER_ID);
            if (!isO1AttrMapper) {
                if (isO2AttrMapper) {
                    return 1;
                } else {
                    return compareWithStableOrdering(o1, o2);
                }
            } else if (!isO2AttrMapper) {
                return -1;
            }

            // Mapper for "username" attribute first
            String model1 = o1.getConfig().getFirst(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE);
            String model2 = o2.getConfig().getFirst(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE);
            boolean isO1UsernameMapper = model1 != null && model1.equalsIgnoreCase(UserModel.USERNAME);
            boolean isO2UsernameMapper = model2 != null && model2.equalsIgnoreCase(UserModel.USERNAME);
            if (!isO1UsernameMapper) {
                if (isO2UsernameMapper) {
                    return 1;
                } else {
                    return compareWithStableOrdering(o1, o2);
                }
            } else if (!isO2UsernameMapper) {
                return -1;
            }

            // The username mapper corresponding to the same like configured username for federationProvider is first
            String o1LdapAttr = o1.getConfig().getFirst(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE);
            String o2LdapAttr = o2.getConfig().getFirst(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE);
            boolean isO1LdapAttr = o1LdapAttr != null && ldapConfig.getUsernameLdapAttribute().equalsIgnoreCase(o1LdapAttr);
            boolean isO2LdapAttr = o2LdapAttr != null && ldapConfig.getUsernameLdapAttribute().equalsIgnoreCase(o2LdapAttr);

            if (!isO1LdapAttr) {
                if (isO2LdapAttr) {
                    return 1;
                } else {
                    return compareWithStableOrdering(o1, o2);
                }
            } else if (!isO2LdapAttr) {
                return -1;
            }

            return compareWithStableOrdering(o1, o2);
        }

        /**
         * Ensure a stable ordering, so the mappers are always executed in the same order.
         * This can avoid database deadlocks as the mappers will modify attributes always in the same order.
         */
        private static int compareWithStableOrdering(ComponentModel o1, ComponentModel o2) {
            return o1.getId().compareTo(o2.getId());
        }

    }


}
