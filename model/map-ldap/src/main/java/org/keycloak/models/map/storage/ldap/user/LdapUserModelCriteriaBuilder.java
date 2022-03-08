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
package org.keycloak.models.map.storage.ldap.user;

import org.keycloak.models.ModelException;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ldap.LdapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.store.LdapMapEscapeStrategy;
import org.keycloak.models.map.storage.ldap.user.config.LdapMapUserMapperConfig;
import org.keycloak.models.map.storage.ldap.user.entity.LdapUserEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.Objects;
import java.util.function.Supplier;

public class LdapUserModelCriteriaBuilder extends LdapModelCriteriaBuilder<LdapUserEntity, UserModel, LdapUserModelCriteriaBuilder> {

    private final LdapMapUserMapperConfig userMapperConfig;

    public String getRealmId() {
        return realmId;
    }

    private String realmId;

    @Override
    public LdapUserModelCriteriaBuilder and(LdapUserModelCriteriaBuilder... builders) {
        LdapUserModelCriteriaBuilder and = super.and(builders);
        for (LdapUserModelCriteriaBuilder builder : builders) {
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
    public LdapUserModelCriteriaBuilder or(LdapUserModelCriteriaBuilder... builders) {
        LdapUserModelCriteriaBuilder or = super.or(builders);
        for (LdapUserModelCriteriaBuilder builder : builders) {
            if (builder.realmId != null) {
                throw new ModelException("realmId not supported in OR condition");
            }
        }
        return or;
    }

    @Override
    public LdapUserModelCriteriaBuilder not(LdapUserModelCriteriaBuilder builder) {
        LdapUserModelCriteriaBuilder not = super.not(builder);
        if (builder.realmId != null) {
            throw new ModelException("realmId not supported in NOT condition");
        }
        return not;
    }

    public LdapUserModelCriteriaBuilder(LdapMapUserMapperConfig userMapperConfig) {
        super(predicateFunc -> new LdapUserModelCriteriaBuilder(userMapperConfig, predicateFunc));
        this.userMapperConfig = userMapperConfig;
    }

    private LdapUserModelCriteriaBuilder(LdapMapUserMapperConfig userMapperConfig, Supplier<StringBuilder> predicateFunc) {
        super(pf -> new LdapUserModelCriteriaBuilder(userMapperConfig, pf), predicateFunc);
        this.userMapperConfig = userMapperConfig;
    }

    @Override
    public LdapUserModelCriteriaBuilder compare(SearchableModelField<? super UserModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField.equals(UserModel.SearchableFields.REALM_ID)) {
                    LdapUserModelCriteriaBuilder result = new LdapUserModelCriteriaBuilder(userMapperConfig, StringBuilder::new);
                    result.realmId = (String) value[0];
                    return result;
                } else if (modelField.equals(UserModel.SearchableFields.CONSENT_FOR_CLIENT) ||
                        modelField.equals(UserModel.SearchableFields.CONSENT_WITH_CLIENT_SCOPE) ||
                        modelField.equals(UserModel.SearchableFields.ASSIGNED_ROLE) ||
                        modelField.equals(UserModel.SearchableFields.ATTRIBUTE) ||
                        modelField.equals(UserModel.SearchableFields.ASSIGNED_GROUP)) {
                    // TODO: don't check on this field in LDAP
                    return new LdapUserModelCriteriaBuilder(userMapperConfig, StringBuilder::new);
                } else if (modelField.equals(UserModel.SearchableFields.USERNAME) || modelField.equals(UserModel.SearchableFields.EMAIL)
                        || modelField.equals(UserModel.SearchableFields.FIRST_NAME) || modelField.equals(UserModel.SearchableFields.LAST_NAME)) {
                    // validateValue(value, modelField, op, String.class);
                    String field = modelFieldNameToLdap(userMapperConfig, modelField);
                    return new LdapUserModelCriteriaBuilder(userMapperConfig,
                            () -> equal(field, value[0], LdapMapEscapeStrategy.DEFAULT, false));
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case NE:
                throw new CriterionNotSupportedException(modelField, op);

            case ILIKE:
            case LIKE:
                if (modelField.equals(UserModel.SearchableFields.USERNAME) || modelField.equals(UserModel.SearchableFields.EMAIL)
                        || modelField.equals(UserModel.SearchableFields.FIRST_NAME) || modelField.equals(UserModel.SearchableFields.LAST_NAME)) {
                    // validateValue(value, modelField, op, String.class);
                    // first escape all elements of the string (which would not escape the percent sign)
                    // then replace percent sign with the wildcard character asterisk
                    // the result should then be used unescaped in the condition.
                    String v = LdapMapEscapeStrategy.DEFAULT.escape(String.valueOf(value[0])).replaceAll("%", "*");
                    // TODO: there is no placeholder for a single character wildcard ... use multicharacter wildcard instead?
                    String field = modelFieldNameToLdap(userMapperConfig, modelField);
                    return new LdapUserModelCriteriaBuilder(userMapperConfig, () -> {
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
                throw new CriterionNotSupportedException(modelField, op);

            case NOT_EXISTS:
                if (modelField.equals(UserModel.SearchableFields.SERVICE_ACCOUNT_CLIENT)) {
                    // TODO: don't check on this field in LDAP
                    return new LdapUserModelCriteriaBuilder(userMapperConfig, StringBuilder::new);
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }

    private String modelFieldNameToLdap(LdapMapUserMapperConfig userMapperConfig, SearchableModelField<? super UserModel> modelField) {
        if (modelField.equals(UserModel.SearchableFields.USERNAME)) {
            return userMapperConfig.getUserNameLdapAttribute();
        } else if (modelField.equals(UserModel.SearchableFields.EMAIL)) {
            return "mail";
        } else if (modelField.equals(UserModel.SearchableFields.FIRST_NAME)) {
            return "cn";
        } else if (modelField.equals(UserModel.SearchableFields.LAST_NAME)) {
            return "sn";
        } else {
            throw new CriterionNotSupportedException(modelField, null);
        }
    }

    public LdapUserModelCriteriaBuilder withCustomFilter(String customFilter) {
        if (customFilter != null && toString().length() > 0) {
            return and(this, new LdapUserModelCriteriaBuilder(userMapperConfig, () -> new StringBuilder(customFilter)));
        }
        return this;
    }
}
