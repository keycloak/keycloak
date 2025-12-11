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
package org.keycloak.services.resources.admin;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.policy.PasswordPolicyNotMetException;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.SearchQueryUtils;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import static org.keycloak.models.utils.KeycloakModelUtils.findGroupByPath;
import static org.keycloak.userprofile.UserProfileContext.USER_API;

/**
 * Base resource for managing users
 *
 * @resource Users
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class UsersResource {

    private static final Logger logger = Logger.getLogger(UsersResource.class);
    private static final String SEARCH_ID_PARAMETER = "id:";

    protected final RealmModel realm;

    private final AdminPermissionEvaluator auth;

    private final AdminEventBuilder adminEvent;

    protected final ClientConnection clientConnection;

    protected final KeycloakSession session;

    protected final HttpHeaders headers;

    public UsersResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.adminEvent = adminEvent.resource(ResourceType.USER);
        this.headers = session.getContext().getRequestHeaders();
    }

    /**
     * Create a new user
     *
     * Username must be unique.
     *
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorRepresentation.class))),
        @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorRepresentation.class)))
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Create a new user Username must be unique.")
    public Response createUser(final UserRepresentation rep) {
        // first check if user has manage rights
        try {
            auth.users().requireManage();
        } catch (ForbiddenException exception) {
            if (!canCreateGroupMembers(rep)) {
                throw exception;
            }
        }

        String username = rep.getUsername();
        if(realm.isRegistrationEmailAsUsername()) {
            username = rep.getEmail();
        }

        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);

        UserProfile profile = profileProvider.create(USER_API, rep.getRawAttributes());

        try {
            Response response = UserResource.validateUserProfile(profile, session, auth.adminAuth());
            if (response != null) {
                return response;
            }

            UserModel user = profile.create();

            UserResource.updateUserFromRep(profile, user, rep, session, false);
            RepresentationToModel.createFederatedIdentities(rep, session, realm, user);
            RepresentationToModel.createGroups(session, rep, realm, user);

            RepresentationToModel.createCredentials(rep, session, realm, user, true);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("User exists with same username or email");
        } catch (PasswordPolicyNotMetException e) {
            logger.warn("Password policy not met for user " + e.getUsername(), e);
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(e.getMessage(), MessageFormat.format(messages.getProperty(e.getMessage(), e.getMessage()), e.getParameters()),
                    Response.Status.BAD_REQUEST);
        } catch (ModelIllegalStateException e) {
            logger.error(e.getMessage(), e);
            throw ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ModelException me){
            logger.warn("Could not create user", me);
            throw ErrorResponse.error("Could not create user", Response.Status.BAD_REQUEST);
        }
    }

    private boolean canCreateGroupMembers(UserRepresentation rep) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ) && !Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
            return false;
        }

        List<GroupModel> groups = Optional.ofNullable(rep.getGroups())
                .orElse(Collections.emptyList())
                .stream().map(path -> findGroupByPath(session, realm, path))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (groups.isEmpty()) {
            return false;
        }

        // if groups is part of the user rep, check if admin has manage_members and manage_membership on each group
        // an exception is thrown in case the current user does not have permissions to manage any of the groups
        for (GroupModel group : groups) {
            auth.groups().requireManageMembers(group);
            auth.groups().requireManageMembership(group);
        }

        return true;
    }

    /**
     * Get representation of the user
     *
     * @param id User id
     * @return
     */
    @Path("{user-id}")
    public UserResource user(final @PathParam("user-id") String id) {
        UserModel user = null;
        if (LightweightUserAdapter.isLightweightUser(id)) {
            UserSessionModel userSession = session.sessions().getUserSession(realm, LightweightUserAdapter.getLightweightUserId(id));
            if (userSession != null) {
                user = userSession.getUser();
            }
        } else {
            user = session.users().getUserById(realm, id);
        }

        if (user == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }

        return new UserResource(session, user, auth, adminEvent);
    }

    /**
     * Get users
     *
     * Returns a stream of users, filtered according to query parameters.
     *
     * @param search A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., <code>foo</code> or <code>foo*</code>). Use <code>*foo*</code> for infix search and <code>"foo"</code> for exact search.
     * @param last A String contained in lastName, or the complete lastName, if param "exact" is true
     * @param first A String contained in firstName, or the complete firstName, if param "exact" is true
     * @param email A String contained in email, or the complete email, if param "exact" is true
     * @param username A String contained in username, or the complete username, if param "exact" is true
     * @param emailVerified whether the email has been verified
     * @param idpAlias The alias of an Identity Provider linked to the user
     * @param idpUserId The userId at an Identity Provider linked to the user
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param enabled Boolean representing if user is enabled or not
     * @param briefRepresentation Boolean which defines whether brief representations are returned (default: false)
     * @param exact Boolean which defines whether the params "last", "first", "email" and "username" must match exactly
     * @param searchQuery A query to search for custom attributes, in the format 'key1:value2 key2:value2'
     * @return a non-null {@code Stream} of users
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Get users Returns a stream of users, filtered according to query parameters.")
    public Stream<UserRepresentation> getUsers(
            @Parameter(description = "A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and \"foo\" for exact search.") @QueryParam("search") String search,
            @Parameter(description = "A String contained in lastName, or the complete lastName, if param \"exact\" is true") @QueryParam("lastName") String last,
            @Parameter(description = "A String contained in firstName, or the complete firstName, if param \"exact\" is true") @QueryParam("firstName") String first,
            @Parameter(description = "A String contained in email, or the complete email, if param \"exact\" is true") @QueryParam("email") String email,
            @Parameter(description = "A String contained in username, or the complete username, if param \"exact\" is true") @QueryParam("username") String username,
            @Parameter(description = "whether the email has been verified") @QueryParam("emailVerified") Boolean emailVerified,
            @Parameter(description = "The alias of an Identity Provider linked to the user") @QueryParam("idpAlias") String idpAlias,
            @Parameter(description = "The userId at an Identity Provider linked to the user") @QueryParam("idpUserId") String idpUserId,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
            @Parameter(description = "Boolean representing if user is enabled or not") @QueryParam("enabled") Boolean enabled,
            @Parameter(description = "Boolean which defines whether brief representations are returned (default: false)") @QueryParam("briefRepresentation") Boolean briefRepresentation,
            @Parameter(description = "Boolean which defines whether the params \"last\", \"first\", \"email\" and \"username\" must match exactly") @QueryParam("exact") Boolean exact,
            @Parameter(description = "A query to search for custom attributes, in the format 'key1:value2 key2:value2'") @QueryParam("q") String searchQuery) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();

        userPermissionEvaluator.requireQuery();

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        Map<String, String> searchAttributes = searchQuery == null
                ? Collections.emptyMap()
                : SearchQueryUtils.getFields(searchQuery);

        Stream<UserModel> userModels = Stream.empty();
        if (search != null) {
            if (search.startsWith(SEARCH_ID_PARAMETER)) {
                String[] userIds = search.substring(SEARCH_ID_PARAMETER.length()).trim().split("\\s+");
                userModels = Arrays.stream(userIds).map(id -> session.users().getUserById(realm, id)).filter(Objects::nonNull);
                if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
                    userModels = userModels.filter(userPermissionEvaluator::canView);
                }
            } else {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(UserModel.SEARCH, search.trim());
                if (enabled != null) {
                    attributes.put(UserModel.ENABLED, enabled.toString());
                }
                if (emailVerified != null) {
                    attributes.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
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

    /**
     * Returns the number of users that match the given criteria.
     * It can be called in three different ways.
     * 1. Don't specify any criteria and pass {@code null}. The number of all
     * users within that realm will be returned.
     * <p>
     * 2. If {@code search} is specified other criteria such as {@code last} will
     * be ignored even though you set them. The {@code search} string will be
     * matched against the first and last name, the username and the email of a
     * user.
     * <p>
     * 3. If {@code search} is unspecified but any of {@code last}, {@code first},
     * {@code email} or {@code username} those criteria are matched against their
     * respective fields on a user entity. Combined with a logical and.
     *
     * @param search A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., <code>foo</code> or <code>foo*</code>). Use <code>*foo*</code> for infix search and <code>"foo"</code> for exact search.
     * @param last A String contained in lastName, or the complete lastName, if param "exact" is true
     * @param first A String contained in firstName, or the complete firstName, if param "exact" is true
     * @param email A String contained in email, or the complete email, if param "exact" is true
     * @param username A String contained in username, or the complete username, if param "exact" is true
     * @param emailVerified whether the email has been verified
     * @param idpAlias The alias of an Identity Provider linked to the user
     * @param idpUserId The userId at an Identity Provider linked to the user
     * @param enabled Boolean representing if user is enabled or not
     * @param exact Boolean which defines whether the params "last", "first", "email" and "username" must match exactly
     * @param searchQuery A query to search for custom attributes, in the format 'key1:value2 key2:value2'
     * @return the number of users that match the given criteria
     */
    @Path("count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Integer.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(
            summary = "Returns the number of users that match the given criteria.",
            description = "It can be called in three different ways. " +
                    "1. Don’t specify any criteria and pass {@code null}. The number of all users within that realm will be returned. <p> " +
                    "2. If {@code search} is specified other criteria such as {@code last} will be ignored even though you set them. The {@code search} string will be matched against the first and last name, the username and the email of a user. <p> " +
                    "3. If {@code search} is unspecified but any of {@code last}, {@code first}, {@code email} or {@code username} those criteria are matched against their respective fields on a user entity. Combined with a logical and.")
    public Integer getUsersCount(
            @Parameter(description = "A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and \"foo\" for exact search.") @QueryParam("search") String search,
            @Parameter(description = "A String contained in lastName, or the complete lastName, if param \"exact\" is true") @QueryParam("lastName") String last,
            @Parameter(description = "A String contained in firstName, or the complete firstName, if param \"exact\" is true") @QueryParam("firstName") String first,
            @Parameter(description = "A String contained in email, or the complete email, if param \"exact\" is true") @QueryParam("email") String email,
            @Parameter(description = "A String contained in username, or the complete username, if param \"exact\" is true") @QueryParam("username") String username,
            @Parameter(description = "whether the email has been verified") @QueryParam("emailVerified") Boolean emailVerified,
            @Parameter(description = "The alias of an Identity Provider linked to the user") @QueryParam("idpAlias") String idpAlias,
            @Parameter(description = "The userId at an Identity Provider linked to the user") @QueryParam("idpUserId") String idpUserId,
            @Parameter(description = "Boolean representing if user is enabled or not") @QueryParam("enabled") Boolean enabled,
            @Parameter(description = "Boolean which defines whether the params \"last\", \"first\", \"email\" and \"username\" must match exactly") @QueryParam("exact") Boolean exact,
            @Parameter(description = "A query to search for custom attributes, in the format 'key1:value2 key2:value2'") @QueryParam("q") String searchQuery) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();

        Map<String, String> searchAttributes = searchQuery == null
                ? Collections.emptyMap()
                : SearchQueryUtils.getFields(searchQuery);
        if (search != null) {
            if (search.startsWith(SEARCH_ID_PARAMETER)) {
                UserModel userModel = session.users().getUserById(realm, search.substring(SEARCH_ID_PARAMETER.length()).trim());
                return userModel != null && userPermissionEvaluator.canView(userModel) ? 1 : 0;
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put(UserModel.SEARCH, search.trim());

            if (enabled != null) {
                parameters.put(UserModel.ENABLED, enabled.toString());
            }
            if (emailVerified != null) {
                parameters.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            // search /users equivalent to this doesn't include service-accounts so counting shouldn't as well
            parameters.put(UserModel.INCLUDE_SERVICE_ACCOUNT, "false");
            if (userPermissionEvaluator.canView()) {
                return session.users().getUsersCount(realm, parameters);
            } else {
                if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
                    return session.users().getUsersCount(realm, parameters, auth.groups().getGroupIdsWithViewPermission());
                } else {
                    return session.users().getUsersCount(realm, parameters);
                }
            }
        } else if (last != null || first != null || email != null || username != null || emailVerified != null || enabled != null || !searchAttributes.isEmpty()) {
            Map<String, String> parameters = new HashMap<>();
            if (last != null) {
                parameters.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                parameters.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                parameters.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                parameters.put(UserModel.USERNAME, username);
            }
            if (emailVerified != null) {
                parameters.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            if (idpAlias != null) {
                parameters.put(UserModel.IDP_ALIAS, idpAlias);
            }
            if (idpUserId != null) {
                parameters.put(UserModel.IDP_USER_ID, idpUserId);
            }
            if (enabled != null) {
                parameters.put(UserModel.ENABLED, enabled.toString());
            }
            if (exact != null) {
                parameters.put(UserModel.EXACT, exact.toString());
            }
            parameters.putAll(searchAttributes);
            // search /users equivalent to this does include service-accounts so we should be explicit
            parameters.put(UserModel.INCLUDE_SERVICE_ACCOUNT, "true");
            if (userPermissionEvaluator.canView()) {
                return session.users().getUsersCount(realm, parameters);
            } else {
                if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
                    return session.users().getUsersCount(realm, parameters, auth.groups().getGroupIdsWithViewPermission());
                } else {
                    return session.users().getUsersCount(realm, parameters);
                }
            }
        } else {
            Map<String, String> parameters = new HashMap<>();
            // list /users equivalent to this doesn't include service-accounts so counting shouldn't as well
            parameters.put(UserModel.INCLUDE_SERVICE_ACCOUNT, "false");
            if (userPermissionEvaluator.canView()) {
                return session.users().getUsersCount(realm, parameters);
            } else {
                if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
                    return session.users().getUsersCount(realm, parameters, auth.groups().getGroupIdsWithViewPermission());
                } else {
                    return session.users().getUsersCount(realm, parameters);
                }
            }
        }
    }

    /**
     * Get representation of the user
     *
     * @param id User id
     * @return
     */
    @Path("profile")
    public UserProfileResource userProfile() {
        return new UserProfileResource(session, auth, adminEvent);
    }

    private Stream<UserRepresentation> searchForUser(Map<String, String> attributes, RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Integer firstResult, Integer maxResults, Boolean includeServiceAccounts) {
        attributes.put(UserModel.INCLUDE_SERVICE_ACCOUNT, includeServiceAccounts.toString());

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            Set<String> groupIds = auth.groups().getGroupIdsWithViewPermission();
            if (!groupIds.isEmpty()) {
                session.setAttribute(UserModel.GROUPS, groupIds);
            }
        }

        return toRepresentation(realm, usersEvaluator, briefRepresentation, session.users().searchForUserStream(realm, attributes, firstResult, maxResults));
    }

    private Stream<UserRepresentation> toRepresentation(RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Stream<UserModel> userModels) {
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;

        if (!AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
            usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);
            userModels = userModels.filter(usersEvaluator::canView);
            usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);
        }

        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);

        return userModels
                .map(user -> {
                    UserProfile profile = provider.create(UserProfileContext.USER_API, user);
                    UserRepresentation rep = profile.toRepresentation(!briefRepresentationB);
                    UserRepresentation userRep = briefRepresentationB ?
                            ModelToRepresentation.toBriefRepresentation(user, rep, false) :
                            ModelToRepresentation.toRepresentation(session, realm, user, rep, false);
                    userRep.setAccess(usersEvaluator.getAccessForListing(user));
                    return userRep;
                });
    }
}
