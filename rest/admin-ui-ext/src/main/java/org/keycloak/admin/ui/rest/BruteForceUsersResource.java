package org.keycloak.admin.ui.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.SearchQueryUtils;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

public class BruteForceUsersResource {
    private static final Logger logger = Logger.getLogger(BruteForceUsersResource.class);
    private static final String SEARCH_ID_PARAMETER = "id:";
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
        if (search != null) {
            if (search.startsWith(SEARCH_ID_PARAMETER)) {
                UserModel userModel =
                        session.users().getUserById(realm, search.substring(SEARCH_ID_PARAMETER.length()).trim());
                if (userModel != null) {
                    userModels = Stream.of(userModel);
                }
            } else {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(UserModel.SEARCH, search.trim());
                if (enabled != null) {
                    attributes.put(UserModel.ENABLED, enabled.toString());
                }
                return searchForUser(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
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

            return searchForUser(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
                    maxResults, true);
        } else {
            return searchForUser(new HashMap<>(), realm, userPermissionEvaluator, briefRepresentation,
                    firstResult, maxResults, false);
        }

        return toRepresentation(realm, userPermissionEvaluator, briefRepresentation, userModels);

    }

    private Stream<BruteUser> searchForUser(Map<String, String> attributes, RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Integer firstResult, Integer maxResults, Boolean includeServiceAccounts) {
        attributes.put(UserModel.INCLUDE_SERVICE_ACCOUNT, includeServiceAccounts.toString());

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            Set<String> groupIds = auth.groups().getGroupIdsWithViewPermission();
            if (!groupIds.isEmpty()) {
                session.setAttribute(UserModel.GROUPS, groupIds);
            }
        }

        return toRepresentation(realm, usersEvaluator, briefRepresentation, session.users().searchForUserStream(realm, attributes, firstResult, maxResults));
    }

    private Stream<BruteUser> toRepresentation(RealmModel realm, UserPermissionEvaluator usersEvaluator,
            Boolean briefRepresentation, Stream<UserModel> userModels) {
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;

        if (!AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
            usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);
            userModels = userModels.filter(usersEvaluator::canView);
            usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);
        }

        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);

        return userModels.map(user -> {
            UserProfile profile = provider.create(UserProfileContext.USER_API, user);
            UserRepresentation rep = profile.toRepresentation(!briefRepresentationB);
            UserRepresentation userRep = briefRepresentationB ?
                    ModelToRepresentation.toBriefRepresentation(user, rep, false) :
                    ModelToRepresentation.toRepresentation(session, realm, user, rep, false);
            userRep.setAccess(usersEvaluator.getAccessForListing(user));
            return userRep;
        }).map(this::getBruteForceStatus);
    }

    private BruteUser getBruteForceStatus(UserRepresentation user) {
        BruteUser bruteUser = new BruteUser(user);
        Map<String, Object> data = new HashMap<>();
        data.put("disabled", false);
        data.put("numFailures", 0);
        data.put("lastFailure", 0);
        data.put("lastIPFailure", "n/a");
        if (!realm.isBruteForceProtected())
            bruteUser.setBruteForceStatus(data);

        UserLoginFailureModel model = session.loginFailures().getUserLoginFailure(realm, user.getId());
        if (model == null) {
            bruteUser.setBruteForceStatus(data);
            return bruteUser;
        }

        boolean disabled;
        disabled = isTemporarilyDisabled(session, realm, user);
        if (disabled) {
            data.put("disabled", true);
        }

        data.put("numFailures", model.getNumFailures());
        data.put("lastFailure", model.getLastFailure());
        data.put("lastIPFailure", model.getLastIPFailure());
        bruteUser.setBruteForceStatus(data);

        return bruteUser;
    }

    public boolean isTemporarilyDisabled(KeycloakSession session, RealmModel realm, UserRepresentation user) {
        UserLoginFailureModel failure = session.loginFailures().getUserLoginFailure(realm, user.getId());
        if (failure != null) {
            int currTime = (int)(Time.currentTimeMillis() / 1000L);
            int failedLoginNotBefore = failure.getFailedLoginNotBefore();
            if (currTime < failedLoginNotBefore) {
                logger.debugv("Current: {0} notBefore: {1}", currTime, failedLoginNotBefore);
                return true;
            }
        }

        return false;
    }
}
