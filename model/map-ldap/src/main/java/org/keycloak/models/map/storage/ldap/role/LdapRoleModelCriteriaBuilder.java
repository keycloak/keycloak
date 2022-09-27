/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.ldap.role;

import org.keycloak.models.ModelException;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ldap.LdapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.store.LdapMapEscapeStrategy;
import org.keycloak.models.map.storage.ldap.role.config.LdapMapRoleMapperConfig;
import org.keycloak.models.map.storage.ldap.role.entity.LdapRoleEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

public class LdapRoleModelCriteriaBuilder extends LdapModelCriteriaBuilder<LdapRoleEntity, RoleModel, LdapRoleModelCriteriaBuilder> {

    private final LdapMapRoleMapperConfig roleMapperConfig;

    public String getClientId() {
        return clientId;
    }

    public Boolean isClientRole() {
        return isClientRole;
    }

    public String getRealmId() {
        return realmId;
    }

    private String clientId;

    private Boolean isClientRole;

    private String realmId;

    @Override
    public LdapRoleModelCriteriaBuilder and(LdapRoleModelCriteriaBuilder... builders) {
        LdapRoleModelCriteriaBuilder and = super.and(builders);
        for (LdapRoleModelCriteriaBuilder builder : builders) {
            if (builder.isClientRole != null) {
                if (and.isClientRole != null && !Objects.equals(and.isClientRole, builder.isClientRole)) {
                    throw new ModelException("isClientRole must be specified in query only once");
                }
                and.isClientRole = builder.isClientRole;
            }
            if (builder.clientId != null) {
                if (and.clientId != null && !Objects.equals(and.clientId, builder.clientId)) {
                    throw new ModelException("clientId must be specified in query only once");
                }
                and.clientId = builder.clientId;
            }
            if (builder.realmId != null) {
                if (and.realmId != null && !Objects.equals(and.realmId, builder.realmId)) {
                    throw new ModelException("realmId must be specified in query only once");
                }
                and.realmId = builder.realmId;
            }
        }
        return and;
    }

    @Override
    public LdapRoleModelCriteriaBuilder or(LdapRoleModelCriteriaBuilder... builders) {
        LdapRoleModelCriteriaBuilder or = super.or(builders);
        for (LdapRoleModelCriteriaBuilder builder : builders) {
            if (builder.isClientRole != null) {
                throw new ModelException("isClientRole not supported in OR condition");
            }
            if (builder.clientId != null) {
                throw new ModelException("clientId not supported in OR condition");
            }
            if (builder.realmId != null) {
                throw new ModelException("realmId not supported in OR condition");
            }
        }
        return or;
    }

    @Override
    public LdapRoleModelCriteriaBuilder not(LdapRoleModelCriteriaBuilder builder) {
        LdapRoleModelCriteriaBuilder not = super.not(builder);
        if (builder.isClientRole != null) {
            throw new ModelException("isClientRole not supported in NOT condition");
        }
        if (builder.clientId != null) {
            throw new ModelException("clientId not supported in NOT condition");
        }
        if (builder.realmId != null) {
            throw new ModelException("realmId not supported in NOT condition");
        }
        return not;
    }

    public LdapRoleModelCriteriaBuilder(LdapMapRoleMapperConfig roleMapperConfig) {
        super(predicateFunc -> new LdapRoleModelCriteriaBuilder(roleMapperConfig, predicateFunc));
        this.roleMapperConfig = roleMapperConfig;
    }

    private LdapRoleModelCriteriaBuilder(LdapMapRoleMapperConfig roleMapperConfig, Supplier<StringBuilder> predicateFunc) {
        super(pf -> new LdapRoleModelCriteriaBuilder(roleMapperConfig, pf), predicateFunc);
        this.roleMapperConfig = roleMapperConfig;
    }

    @Override
    public LdapRoleModelCriteriaBuilder compare(SearchableModelField<? super RoleModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField == RoleModel.SearchableFields.IS_CLIENT_ROLE) {
                    LdapRoleModelCriteriaBuilder result = new LdapRoleModelCriteriaBuilder(roleMapperConfig, StringBuilder::new);
                    result.isClientRole = (boolean) value[0];
                    return result;
                } else if (modelField == RoleModel.SearchableFields.CLIENT_ID) {
                    LdapRoleModelCriteriaBuilder result = new LdapRoleModelCriteriaBuilder(roleMapperConfig, StringBuilder::new);
                    result.clientId = (String) value[0];
                    return result;
                } else if (modelField == RoleModel.SearchableFields.REALM_ID) {
                    LdapRoleModelCriteriaBuilder result = new LdapRoleModelCriteriaBuilder(roleMapperConfig, StringBuilder::new);
                    result.realmId = (String) value[0];
                    return result;
                } else if (modelField == RoleModel.SearchableFields.NAME) {
                    // validateValue(value, modelField, op, String.class);
                    String field = modelFieldNameToLdap(roleMapperConfig, modelField);
                    return new LdapRoleModelCriteriaBuilder(roleMapperConfig, 
                            () -> equal(field, value[0], LdapMapEscapeStrategy.DEFAULT, false));
                } else if (modelField == RoleModel.SearchableFields.COMPOSITE_ROLE) {
                    // Not supported at the moment
                    return new LdapRoleModelCriteriaBuilder(roleMapperConfig, StringBuilder::new);
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case NE:
                if (modelField == RoleModel.SearchableFields.IS_CLIENT_ROLE) {
                    LdapRoleModelCriteriaBuilder result = new LdapRoleModelCriteriaBuilder(roleMapperConfig, StringBuilder::new);
                    result.isClientRole = !((boolean) value[0]);
                    return result;
                } else if (modelField == RoleModel.SearchableFields.NAME) {
                    // validateValue(value, modelField, op, String.class);
                    String field = modelFieldNameToLdap(roleMapperConfig, modelField);
                    return not(new LdapRoleModelCriteriaBuilder(roleMapperConfig, 
                            () -> equal(field, value[0], LdapMapEscapeStrategy.DEFAULT, false)));
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case ILIKE:
            case LIKE:
                if (modelField == RoleModel.SearchableFields.NAME ||
                     modelField == RoleModel.SearchableFields.DESCRIPTION) {
                    // validateValue(value, modelField, op, String.class);
                    // first escape all elements of the string (which would not escape the percent sign)
                    // then replace percent sign with the wildcard character asterisk
                    // the result should then be used unescaped in the condition.
                    String v = LdapMapEscapeStrategy.DEFAULT.escape(String.valueOf(value[0])).replaceAll("%", "*");
                    // TODO: there is no placeholder for a single character wildcard ... use multicharacter wildcard instead?
                    String field = modelFieldNameToLdap(roleMapperConfig, modelField);
                    return new LdapRoleModelCriteriaBuilder(roleMapperConfig, () -> {
                        if (v.equals("**")) {
                            // wildcard everything is not well-understood by LDAP and will result in "ERR_01101_NULL_LENGTH The length should not be 0"
                            return new StringBuilder();
                        } else {
                            return equal(field, v, LdapMapEscapeStrategy.NON_ASCII_CHARS_ONLY, false);
                        }
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case IN:
                if (modelField == RoleModel.SearchableFields.NAME ||
                        modelField == RoleModel.SearchableFields.DESCRIPTION ||
                        modelField == RoleModel.SearchableFields.ID) {
                    String field = modelFieldNameToLdap(roleMapperConfig, modelField);
                    return new LdapRoleModelCriteriaBuilder(roleMapperConfig, () -> {
                        Object[] v;
                        if (value[0] instanceof ArrayList) {
                            v = ((ArrayList<?>) value[0]).toArray();
                        } else {
                            throw new CriterionNotSupportedException(modelField, op);
                        }
                        return in(field, v, false);
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }

    private String modelFieldNameToLdap(LdapMapRoleMapperConfig roleMapperConfig, SearchableModelField<? super RoleModel> modelField) {
        if (modelField == RoleModel.SearchableFields.NAME) {
            return roleMapperConfig.getRoleNameLdapAttribute();
        } else if (modelField == RoleModel.SearchableFields.ID) {
            return roleMapperConfig.getLdapMapConfig().getUuidLDAPAttributeName();
        } else if (modelField == RoleModel.SearchableFields.DESCRIPTION) {
            return "description";
        } else {
            throw new CriterionNotSupportedException(modelField, null);
        }
    }

    public LdapRoleModelCriteriaBuilder withCustomFilter(String customFilter) {
        if (customFilter != null && toString().length() > 0) {
            return and(this, new LdapRoleModelCriteriaBuilder(roleMapperConfig, () -> new StringBuilder(customFilter)));
        }
        return this;
    }
}
