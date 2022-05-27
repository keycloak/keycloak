/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.user;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;

import org.keycloak.models.UserModel;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaPredicateFunction;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserAttributeEntity;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserConsentEntity;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserEntity;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserFederatedIdentityEntity;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.storage.StorageId;

/**
 * A {@link JpaModelCriteriaBuilder} implementation for users.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaUserModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaUserEntity, UserModel, JpaUserModelCriteriaBuilder> {

    public JpaUserModelCriteriaBuilder() {
        super(JpaUserModelCriteriaBuilder::new);
    }

    private JpaUserModelCriteriaBuilder(final JpaPredicateFunction<JpaUserEntity> predicateFunc) {
        super(JpaUserModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaUserModelCriteriaBuilder compare(SearchableModelField<? super UserModel> modelField, Operator op, Object... value) {
        switch(op) {
            case EQ:
                if (modelField == UserModel.SearchableFields.REALM_ID ||
                    modelField == UserModel.SearchableFields.USERNAME ||
                    modelField == UserModel.SearchableFields.EMAIL ||
                    modelField == UserModel.SearchableFields.FEDERATION_LINK) {

                    validateValue(value, modelField, op, String.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(root.get(modelField.getName()), value[0])
                    );

                } else if (modelField == UserModel.SearchableFields.ENABLED ||
                    modelField == UserModel.SearchableFields.EMAIL_VERIFIED) {

                    validateValue(value, modelField, op, Boolean.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(root.get(modelField.getName()), value[0])
                    );

                } else if (modelField == UserModel.SearchableFields.IDP_AND_USER) {

                    if (value == null || value.length == 0 || value.length > 2) {
                        throw new CriterionNotSupportedException(modelField, op,
                                "Invalid arguments, expected (idp_alias) or (idp_alias, idp_user), got: " + Arrays.toString(value));
                    }

                    if (value.length == 1) {
                        // search by idp only
                        return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                                cb.equal(root.join("federatedIdentities", JoinType.LEFT).get("identityProvider"),
                                        value[0]));
                    } else if (value[0] == null) {
                        // search by userid only
                        return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                                cb.equal(root.join("federatedIdentities", JoinType.LEFT).get("userId"),
                                        value[1]));
                    } else {
                        // search using both idp and userid
                        return new JpaUserModelCriteriaBuilder((cb, query, root) -> {
                                Join<JpaUserEntity, JpaUserFederatedIdentityEntity> join =
                                        root.join("federatedIdentities", JoinType.LEFT);
                                return cb.and(cb.equal(join.get("identityProvider"), value[0]),
                                            cb.equal(join.get("userId"),value[1]));
                        });
                    }

                } else if (modelField == UserModel.SearchableFields.ASSIGNED_GROUP) {
                    validateValue(value, modelField, op, String.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(root.join("groupIds", JoinType.LEFT), value[0]));

                } else if (modelField == UserModel.SearchableFields.ASSIGNED_ROLE) {
                    validateValue(value, modelField, op, String.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(root.join("roleIds", JoinType.LEFT), value[0]));

                } else if (modelField == UserModel.SearchableFields.SERVICE_ACCOUNT_CLIENT) {
                    validateValue(value, modelField, op, String.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(
                                    cb.function("->>", String.class, root.get("metadata"), cb.literal("fServiceAccountClientLink")),
                                    value[0]));

                } else if (modelField == UserModel.SearchableFields.CONSENT_FOR_CLIENT) {
                    validateValue(value, modelField, op, String.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                            cb.equal(root.join("consents", JoinType.LEFT).get("clientId"), value[0]));

                } else if (modelField == UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK) {
                    validateValue(value, modelField, op, String.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) -> {
                        String providerId = new StorageId((String) value[0], "").getId() + "%";
                        return  cb.like(root.join("consents", JoinType.LEFT).get("clientId"), providerId);
                    });

                } else if (modelField == UserModel.SearchableFields.CONSENT_WITH_CLIENT_SCOPE) {
                    validateValue(value, modelField, op, String.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) -> {
                       Join<JpaUserEntity, JpaUserConsentEntity> join = root.join("consents", JoinType.LEFT);
                       return cb.isTrue(cb.function("@>",
                               Boolean.TYPE,
                               cb.function("->", JsonbType.class, join.get("metadata"), cb.literal("fGrantedClientScopesIds")),
                               cb.literal(convertToJson(value[0]))));
                    });

                } else if (modelField == UserModel.SearchableFields.ATTRIBUTE) {
                    validateValue(value, modelField, op, String.class, String.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) -> {
                        Join<JpaUserEntity, JpaUserAttributeEntity> join = root.join("attributes", JoinType.LEFT);
                        return cb.and(
                                cb.equal(join.get("name"), value[0]),
                                cb.equal(join.get("value"), value[1])
                        );
                    });
                }
                else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case ILIKE:
                if (modelField == UserModel.SearchableFields.USERNAME ||
                    modelField == UserModel.SearchableFields.FIRST_NAME ||
                    modelField == UserModel.SearchableFields.LAST_NAME ||
                    modelField == UserModel.SearchableFields.EMAIL) {

                    validateValue(value, modelField, op, String.class);
                    return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                            cb.like(cb.lower(root.get(modelField.getName())), value[0].toString().toLowerCase()));
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case IN:
                if (modelField == UserModel.SearchableFields.ASSIGNED_GROUP) {
                    final Collection<?> collectionValues = getValuesForInOperator(value, modelField);
                    if (collectionValues.isEmpty()) return new JpaUserModelCriteriaBuilder((cb, query, root) -> cb.or());

                    return new JpaUserModelCriteriaBuilder((cb, query, root) -> {
                        CriteriaBuilder.In<String> in = cb.in(root.join("groupIds", JoinType.LEFT));
                        collectionValues.forEach(groupId -> {
                            if (!(groupId instanceof String))
                                throw new CriterionNotSupportedException(modelField, op, "Invalid type. Expected String, got " + groupId.getClass());
                            in.value(groupId.toString());
                        });
                        return in;
                    });

                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            case NOT_EXISTS:
                if (modelField == UserModel.SearchableFields.SERVICE_ACCOUNT_CLIENT) {
                    return new JpaUserModelCriteriaBuilder((cb, query, root) ->
                        cb.isNull(cb.function("->>", String.class, root.get("metadata"), cb.literal("fServiceAccountClientLink"))));
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }
            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }
}
