package org.keycloak.admin.ui.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.keycloak.admin.ui.rest.model.BruteUser;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.BruteForceUserProperty;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;
import org.keycloak.utils.SearchQueryUtils;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

public class BruteForceUsersResource {
    private static final Logger logger = Logger.getLogger(BruteForceUsersResource.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public BruteForceUsersResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.realm = realm;
        this.auth = auth;
        this.session = session;
    }

    @GET
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Operation(
            summary = "Find all users and add if they are locked by brute force protection",
            description = "Same endpoint as the users search but added brute force protection status."
    )
    @APIResponse(
            responseCode = "200",
            description = "",
            content = {@Content(
                    schema = @Schema(
                            implementation = BruteUser.class,
                            type = SchemaType.ARRAY
                    )
            )}
    )
    public final Stream<BruteUser> searchUser(@QueryParam("search") String search,
            @QueryParam("lastName") String last,
            @QueryParam("firstName") String first,
            @QueryParam("email") String email,
            @QueryParam("username") String username,
            @QueryParam("emailVerified") Boolean emailVerified,
            @QueryParam("idpAlias") String idpAlias,
            @QueryParam("idpUserId") String idpUserId,
            @QueryParam("first") @DefaultValue("-1") Integer firstResult,
            @QueryParam("max") @DefaultValue("" + Constants.DEFAULT_MAX_RESULTS) Integer maxResults,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("briefRepresentation") Boolean briefRepresentation,
            @QueryParam("exact") Boolean exact,
            @QueryParam("q") String searchQuery) {
        final UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();

        Map<String, String> searchAttributes = searchQuery == null
                ? Collections.emptyMap()
                : SearchQueryUtils.getFields(searchQuery);

        Stream<UserModel> userModels = Stream.empty();
        boolean briefRep = Boolean.TRUE.equals(briefRepresentation);

        if (search != null) {
            SearchQueryUtils.UserSearchPrefix prefix = SearchQueryUtils.UserSearchPrefix.matching(search);
            if (prefix != null) {
                userModels = Arrays.stream(prefix.splitTerms(search))
                        .map(term -> prefix.lookup(session.users(), realm, term))
                        .filter(Objects::nonNull);
                if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
                    userModels = userModels.filter(userPermissionEvaluator::canView);
                }
            } else {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(UserModel.SEARCH, search.trim());
                if (enabled != null) {
                    attributes.put(UserModel.ENABLED, enabled.toString());
                }
                return searchForUser(attributes, realm, userPermissionEvaluator, briefRep, firstResult,
                        maxResults, false);
            }
        } else if (last != null || first != null || email != null || username != null || emailVerified != null
                || idpAlias != null || idpUserId != null || enabled != null || exact != null || !searchAttributes.isEmpty()) {
            Map<String, String> attributes = new HashMap<>();
            if (last != null) {
                attributes.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                attributes.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                attributes.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                attributes.put(UserModel.USERNAME, username);
            }
            if (emailVerified != null) {
                attributes.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            if (idpAlias != null) {
                attributes.put(UserModel.IDP_ALIAS, idpAlias);
            }
            if (idpUserId != null) {
                attributes.put(UserModel.IDP_USER_ID, idpUserId);
            }
            if (enabled != null) {
                attributes.put(UserModel.ENABLED, enabled.toString());
            }
            if (exact != null) {
                attributes.put(UserModel.EXACT, exact.toString());
            }

            attributes.putAll(searchAttributes);

            return searchForUser(attributes, realm, userPermissionEvaluator, briefRep, firstResult,
                    maxResults, true);
        } else {
            return searchForUser(new HashMap<>(), realm, userPermissionEvaluator, briefRep,
                    firstResult, maxResults, false);
        }

        return toRepresentation(realm, userPermissionEvaluator, briefRep, userModels);

    }

    private Stream<BruteUser> searchForUser(Map<String, String> attributes, RealmModel realm, UserPermissionEvaluator usersEvaluator, boolean briefRep, Integer firstResult, Integer maxResults, Boolean includeServiceAccounts) {
        attributes.put(UserModel.INCLUDE_SERVICE_ACCOUNT, includeServiceAccounts.toString());

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            Set<String> groupIds = auth.groups().getGroupIdsWithViewPermission();
            if (!groupIds.isEmpty()) {
                session.setAttribute(UserModel.GROUPS, groupIds);
            }
        }

        return toRepresentation(realm, usersEvaluator, briefRep, session.users().searchForUserStream(realm, attributes, firstResult, maxResults));
    }

    private Stream<BruteUser> toRepresentation(RealmModel realm, UserPermissionEvaluator usersEvaluator,
            boolean briefRep, Stream<UserModel> userModels) {
        if (!AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
            usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);
            userModels = userModels.filter(usersEvaluator::canView);
            usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);
        }

        return userModels.map(user -> {
            UserRepresentation userRep = ModelToRepresentation.toRepresentation(session, user, briefRep);
            userRep.setAccess(usersEvaluator.getAccessForListing(user));
            return getBruteForceStatus(user, userRep);
        });
    }

    private BruteUser getBruteForceStatus(UserModel user, UserRepresentation representation) {
        BruteUser bruteUser = new BruteUser(representation);
        Map<String, Object> data = new HashMap<>();
        data.put("disabled", false);
        data.put("numFailures", 0);
        data.put("lastFailure", 0);
        data.put("lastIPFailure", "n/a");
        if (!realm.isBruteForceProtected()) {
            bruteUser.setBruteForceStatus(data);
            return bruteUser;
        }

        UserLoginFailureModel latestFailure = null;
        boolean disabled = session.getProvider(BruteForceProtector.class)
                .isPermanentlyLockedOut(session, realm, user);
        int currentTime = Time.currentTime();
        for (UserLoginFailureModel model : BruteForceUserProperty.getLoginFailures(session, realm, user).toList()) {
            data.put("numFailures", Math.max((int) data.get("numFailures"), model.getNumFailures()));
            if (latestFailure == null || model.getLastFailure() > latestFailure.getLastFailure()) {
                latestFailure = model;
            }
            if (currentTime < model.getFailedLoginNotBefore()) {
                logger.debugv("Current: {0} notBefore: {1}", currentTime, model.getFailedLoginNotBefore());
                disabled = true;
            }
        }

        if (latestFailure == null) {
            bruteUser.setBruteForceStatus(data);
            return bruteUser;
        }
        data.put("disabled", disabled);
        data.put("lastFailure", latestFailure.getLastFailure());
        data.put("lastIPFailure", latestFailure.getLastIPFailure());
        bruteUser.setBruteForceStatus(data);

        return bruteUser;
    }
}
