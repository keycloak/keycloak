package org.keycloak.models.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.storage.jpa.JpaHashUtils;

import static org.keycloak.models.UserModel.EMAIL;
import static org.keycloak.models.UserModel.EMAIL_VERIFIED;
import static org.keycloak.models.UserModel.FIRST_NAME;
import static org.keycloak.models.UserModel.LAST_NAME;
import static org.keycloak.models.UserModel.USERNAME;

public final class JpaUserUtils {

    private static final char ESCAPE_BACKSLASH = '\\';

    public static List<Predicate> createPredicates(KeycloakSession session,
                                                   CriteriaBuilder builder,
                                                   CriteriaQuery<?> query,
                                                   Map<String, String> attributes, From<?, UserEntity> root,
                                                   Boolean exact, Map<String, String> customLongValueSearchAttributes) {
        List<Predicate> predicates = new ArrayList<>();
        List<Predicate> attributePredicates = new ArrayList<>();
        Join<Object, Object> federatedIdentitiesJoin = null;

        exact = Optional.ofNullable(exact).orElse(false);

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) {
                continue;
            }

            switch (key) {
                case UserModel.SEARCH:
                    addSearchPredicates(value, exact, builder, root, predicates);
                    break;
                case FIRST_NAME:
                case LAST_NAME:
                    if (exact) {
                        predicates.add(builder.equal(builder.lower(root.get(key)), value.toLowerCase()));
                    } else {
                        predicates.add(builder.like(builder.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
                    }
                    break;
                case USERNAME:
                case EMAIL:
                    if (exact) {
                        predicates.add(builder.equal(root.get(key), value.toLowerCase()));
                    } else {
                        predicates.add(builder.like(root.get(key), "%" + value.toLowerCase() + "%"));
                    }
                    break;
                case EMAIL_VERIFIED:
                    predicates.add(builder.equal(root.get(key), Boolean.valueOf(value.toLowerCase())));
                    break;
                case UserModel.ENABLED:
                    predicates.add(builder.equal(root.get(key), Boolean.valueOf(value)));
                    break;
                case UserModel.IDP_ALIAS:
                    if (federatedIdentitiesJoin == null) {
                        federatedIdentitiesJoin = root.join("federatedIdentities");
                    }
                    predicates.add(builder.equal(federatedIdentitiesJoin.get("identityProvider"), value));
                    break;
                case UserModel.IDP_USER_ID:
                    if (federatedIdentitiesJoin == null) {
                        federatedIdentitiesJoin = root.join("federatedIdentities");
                    }
                    predicates.add(builder.equal(federatedIdentitiesJoin.get("userId"), value));
                    break;
                case UserModel.EXACT:
                    break;
                case UserModel.INCLUDE_SERVICE_ACCOUNT: {
                    if (!attributes.containsKey(UserModel.INCLUDE_SERVICE_ACCOUNT)
                            || !Boolean.parseBoolean(attributes.get(UserModel.INCLUDE_SERVICE_ACCOUNT))) {
                        predicates.add(root.get("serviceAccountClientLink").isNull());
                    }
                    break;
                }
                default:
                    // All unknown attributes will be assumed as custom attributes
                    Join<UserEntity, UserAttributeEntity> attributesJoin = root.join("attributes", JoinType.LEFT);
                    if (value.length() > 255) {
                        customLongValueSearchAttributes.put(key, value);
                        attributePredicates.add(builder.and(
                                builder.equal(attributesJoin.get("name"), key),
                                builder.equal(attributesJoin.get("longValueHashLowerCase"), JpaHashUtils.hashForAttributeValueLowerCase(value))));
                    } else {
                        if (Boolean.parseBoolean(attributes.getOrDefault(UserModel.EXACT, Boolean.TRUE.toString()))) {
                            attributePredicates.add(builder.and(
                                    builder.equal(attributesJoin.get("name"), key),
                                    builder.equal(builder.lower(attributesJoin.get("value")), value.toLowerCase())));
                        } else {
                            attributePredicates.add(builder.and(
                                    builder.equal(attributesJoin.get("name"), key),
                                    builder.like(builder.lower(attributesJoin.get("value")), "%" + value.toLowerCase() + "%")));
                        }
                    }
                    break;
            }
        }

        if (!attributePredicates.isEmpty()) {
            predicates.add(builder.and(attributePredicates));
        }

        RealmModel realm = session.getContext().getRealm();
        JpaUserPartialEvaluationProvider partialEvaluator = (JpaUserPartialEvaluationProvider) session.getProvider(UserProvider.class, "jpa");

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(
                session,
                AdminPermissionsSchema.USERS,
                partialEvaluator,
                realm,
                builder,
                query,
                root));

        return predicates;
    }

    public static void addSearchPredicates(String search, boolean exact, CriteriaBuilder builder, From<?, UserEntity> from, List<Predicate> predicates) {
        if (search == null) {
            return;
        }
        for (String stringToSearch : search.trim().split("\\s+")) {
            predicates.add(getSearchOptionPredicate(stringToSearch, exact, builder, from));
        }
    }

    private static Predicate getSearchOptionPredicate(String value, boolean exact, CriteriaBuilder builder, From<?, UserEntity> from) {
        if (!exact) {
            value = value.toLowerCase();
        }

        List<Predicate> orPredicates = new ArrayList<>();

        if (exact || value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            if (!exact) {
                value = value.substring(1, value.length() - 1);
            }

            // exact search
            orPredicates.add(builder.equal(from.get(USERNAME), value));
            orPredicates.add(builder.equal(from.get(EMAIL), value));

            if (exact) {
                orPredicates.add(builder.equal(from.get(FIRST_NAME), value));
                orPredicates.add(builder.equal(from.get(LAST_NAME), value));
            } else {
                orPredicates.add(builder.equal(builder.lower(from.get(FIRST_NAME)), value));
                orPredicates.add(builder.equal(builder.lower(from.get(LAST_NAME)), value));
            }
        } else {
            value = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            value = value.replace("*", "%");

            if (value.isEmpty() || value.charAt(value.length() - 1) != '%') value += "%";

            orPredicates.add(builder.like(from.get(USERNAME), value, ESCAPE_BACKSLASH));
            orPredicates.add(builder.like(from.get(EMAIL), value, ESCAPE_BACKSLASH));
            orPredicates.add(builder.like(builder.lower(from.get(FIRST_NAME)), value, ESCAPE_BACKSLASH));
            orPredicates.add(builder.like(builder.lower(from.get(LAST_NAME)), value, ESCAPE_BACKSLASH));
        }

        return builder.or(orPredicates);
    }
}
