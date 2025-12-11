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

package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.UserRepresentation;

public interface UsersResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("username") String username,
                                    @QueryParam("firstName") String firstName,
                                    @QueryParam("lastName") String lastName,
                                    @QueryParam("email") String email,
                                    @QueryParam("first") Integer firstResult,
                                    @QueryParam("max") Integer maxResults);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("username") String username,
                                    @QueryParam("firstName") String firstName,
                                    @QueryParam("lastName") String lastName,
                                    @QueryParam("email") String email,
                                    @QueryParam("first") Integer firstResult,
                                    @QueryParam("max") Integer maxResults,
                                    @QueryParam("enabled") Boolean enabled,
                                    @QueryParam("briefRepresentation") Boolean briefRepresentation);

    /**
     * Search for users based on the given filters.
     *
     * @param username a value contained in username
     * @param firstName a value contained in first name
     * @param lastName a value contained in last name
     * @param email a value contained in email
     * @param emailVerified whether the email has been verified
     * @param idpAlias the alias of the Identity Provider
     * @param idpUserId the userId at the Identity Provider
     * @param firstResult the position of the first result to retrieve
     * @param maxResults the maximum number of results to retrieve
     * @param enabled only return enabled or disabled users
     * @param briefRepresentation Only return basic information (only guaranteed to return id, username, created, first
     *        and last name, email, enabled state, email verification state, federation link, and access.
     *        Note that it means that namely user attributes, required actions, and not before are not returned.)
     * @return a list of {@link UserRepresentation}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("username") String username,
                                    @QueryParam("firstName") String firstName,
                                    @QueryParam("lastName") String lastName,
                                    @QueryParam("email") String email,
                                    @QueryParam("emailVerified") Boolean emailVerified,
                                    @QueryParam("idpAlias") String idpAlias,
                                    @QueryParam("idpUserId") String idpUserId,
                                    @QueryParam("first") Integer firstResult,
                                    @QueryParam("max") Integer maxResults,
                                    @QueryParam("enabled") Boolean enabled,
                                    @QueryParam("briefRepresentation") Boolean briefRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("username") String username,
                                    @QueryParam("firstName") String firstName,
                                    @QueryParam("lastName") String lastName,
                                    @QueryParam("email") String email,
                                    @QueryParam("emailVerified") Boolean emailVerified,
                                    @QueryParam("first") Integer firstResult,
                                    @QueryParam("max") Integer maxResults,
                                    @QueryParam("enabled") Boolean enabled,
                                    @QueryParam("briefRepresentation") Boolean briefRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("emailVerified") Boolean emailVerified,
                                    @QueryParam("first") Integer firstResult,
                                    @QueryParam("max") Integer maxResults,
                                    @QueryParam("enabled") Boolean enabled,
                                    @QueryParam("briefRepresentation") Boolean briefRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("username") String username);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<UserRepresentation> searchByAttributes(@QueryParam("q") String searchQuery);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<UserRepresentation> searchByAttributes(@QueryParam("q") String searchQuery,
                                                @QueryParam("exact") Boolean exact);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<UserRepresentation> searchByAttributes(@QueryParam("first") Integer firstResult,
                                                @QueryParam("max") Integer maxResults,
                                                @QueryParam("enabled") Boolean enabled,
                                                @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                                @QueryParam("q") String searchQuery);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<UserRepresentation> searchByAttributes(@QueryParam("first") Integer firstResult,
                                                @QueryParam("max") Integer maxResults,
                                                @QueryParam("enabled") Boolean enabled,
                                                @QueryParam("exact") Boolean exact,
                                                @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                                @QueryParam("q") String searchQuery);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("username") String username, @QueryParam("exact") Boolean exact);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> searchByUsername(@QueryParam("username") String username, @QueryParam("exact") Boolean exact);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> searchByEmail(@QueryParam("email") String email, @QueryParam("exact") Boolean exact);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> searchByFirstName(@QueryParam("firstName") String email, @QueryParam("exact") Boolean exact);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> searchByLastName(@QueryParam("lastName") String email, @QueryParam("exact") Boolean exact);

    /**
     * Search for users based on the given filters.
     *
     * @param username a value contained in username
     * @param firstName a value contained in first name
     * @param lastName a value contained in last name
     * @param email a value contained in email
     * @param firstResult the position of the first result to retrieve
     * @param maxResults the maximum number of results to retrieve
     * @param enabled only return enabled or disabled users
     * @param briefRepresentation Only return basic information (only guaranteed to return id, username, created, first
     *        and last name, email, enabled state, email verification state, federation link, and access.
     *        Note that it means that namely user attributes, required actions, and not before are not returned.)
     * @param exact search with exact matching by filters (username, email, firstName, lastName)
     * @return a list of {@link UserRepresentation}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("username") String username,
                                    @QueryParam("firstName") String firstName,
                                    @QueryParam("lastName") String lastName,
                                    @QueryParam("email") String email,
                                    @QueryParam("first") Integer firstResult,
                                    @QueryParam("max") Integer maxResults,
                                    @QueryParam("enabled") Boolean enabled,
                                    @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                    @QueryParam("exact") Boolean exact);

    /**
     * Search for users whose username or email matches the value provided by {@code search}. The {@code search}
     * argument also allows finding users by specific attributes as follows:
     *
     * <ul>
     *     <li><i>id:</i> - Find users by identifier. For instance, <i>id:aa497859-bbf5-44ac-bf1a-74dbffcaf197</i></li>
     * </ul>
     *
     * @param search the value to search. It can be the username, email or any of the supported options to query based on user attributes
     * @param firstResult the position of the first result to retrieve
     * @param maxResults the maximum number of results to retrieve
     * @return a list of {@link UserRepresentation}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("search") String search,
                                    @QueryParam("first") Integer firstResult,
                                    @QueryParam("max") Integer maxResults);

    /**
     * Search for users whose username or email matches the value provided by {@code search}. The {@code search}
     * argument also allows finding users by specific attributes as follows:
     *
     * <ul>
     *     <li><i>id:</i> - Find users by identifier. For instance, <i>id:aa497859-bbf5-44ac-bf1a-74dbffcaf197</i></li>
     * </ul>
     *
     * @param search the value to search. It can be the username, email or any of the supported options to query based on user attributes
     * @param firstResult the position of the first result to retrieve
     * @param maxResults the maximum number of results to retrieve
     * @param briefRepresentation Only return basic information (only guaranteed to return id, username, created, first and last name,
     *      email, enabled state, email verification state, federation link, and access.
     *      Note that it means that namely user attributes, required actions, and not before are not returned.)
     * @return a list of {@link UserRepresentation}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("search") String search,
                                    @QueryParam("first") Integer firstResult,
                                    @QueryParam("max") Integer maxResults,
                                    @QueryParam("briefRepresentation") Boolean briefRepresentation);

    /**
     * Search for users whose username, first or last name or email matches the value provided by {@code search}. The {@code search}
     * argument also allows finding users by specific attributes as follows:
     *
     * <ul>
     *     <li><i>id:</i> - Find users by identifier. For instance, <i>id:aa497859-bbf5-44ac-bf1a-74dbffcaf197</i></li>
     * </ul>
     *
     * @param search the value to search. It can be the username, email or any of the supported options to query based on user attributes
     * @param enabled if true, only users that are enabled are returned
     * @param firstResult the position of the first result to retrieve
     * @param maxResults the maximum number of results to retrieve
     * @return a list of {@link UserRepresentation}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("search") String search,
      @QueryParam("enabled") Boolean enabled,
      @QueryParam("first") Integer firstResult,
      @QueryParam("max") Integer maxResults);

    /**
     * Returns the users that can be viewed and match the given filters.
     *
     * @param search        arbitrary search string for all the fields below
     * @param last          last name field of a user
     * @param first         first name field of a user
     * @param email         email field of a user
     * @param emailVerified emailVerified field of a user
     * @param username      username field of a user
     * @param enabled       Boolean representing if user is enabled or not
     * @return the list of users matching the given filters
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("search") String search,
                  @QueryParam("lastName") String last,
                  @QueryParam("firstName") String first,
                  @QueryParam("email") String email,
                  @QueryParam("emailVerified") Boolean emailVerified,
                  @QueryParam("username") String username,
                  @QueryParam("enabled") Boolean enabled,
                  @QueryParam("q") String searchQuery);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> list(@QueryParam("first") Integer firstResult,
                                  @QueryParam("max") Integer maxResults);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> list();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(UserRepresentation userRepresentation);

    /**
     * Returns the number of users that can be viewed.
     *
     * @return number of users
     */
    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Integer count();

    /**
     * Returns the number of users that can be viewed and match the given search criteria.
     * If none is specified this is equivalent to {{@link #count()}}.
     *
     * @param search criteria to search for
     * @return number of users matching the search criteria
     */
    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Integer count(@QueryParam("search") String search);

    /**
     * Returns the number of users that can be viewed and match the given filters.
     * If none of the filters is specified this is equivalent to {{@link #count()}}.
     *
     * @param last     last name field of a user
     * @param first    first name field of a user
     * @param email    email field of a user
     * @param username username field of a user
     * @return number of users matching the given filters
     */
    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Integer count(@QueryParam("lastName") String last,
                  @QueryParam("firstName") String first,
                  @QueryParam("email") String email,
                  @QueryParam("username") String username);

    /**
     * Returns the number of users that can be viewed and match the given filters.
     * If none of the filters is specified this is equivalent to {{@link #count()}}.
     *
     * @param last          last name field of a user
     * @param first         first name field of a user
     * @param email         email field of a user
     * @param emailVerified emailVerified field of a user
     * @param username      username field of a user
     * @return number of users matching the given filters
     */
    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Integer count(@QueryParam("lastName") String last,
                  @QueryParam("firstName") String first,
                  @QueryParam("email") String email,
                  @QueryParam("emailVerified") Boolean emailVerified,
                  @QueryParam("username") String username);

    /**
     * Returns the number of users that can be viewed and match the given filters.
     * If none of the filters is specified this is equivalent to {{@link #count()}}.
     *
     * @param search        arbitrary search string for all the fields below
     * @param last          last name field of a user
     * @param first         first name field of a user
     * @param email         email field of a user
     * @param emailVerified emailVerified field of a user
     * @param username      username field of a user
     * @param enabled       Boolean representing if user is enabled or not
     * @return number of users matching the given filters
     */
    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Integer count(@QueryParam("search") String search,
                  @QueryParam("lastName") String last,
                  @QueryParam("firstName") String first,
                  @QueryParam("email") String email,
                  @QueryParam("emailVerified") Boolean emailVerified,
                  @QueryParam("username") String username,
                  @QueryParam("enabled") Boolean enabled,
                  @QueryParam("q") String searchQuery);

    /**
     * Returns the number of users that can be viewed and match the given filters.
     * If none of the filters is specified this is equivalent to {{@link #count()}}.
     *
     * @param search        arbitrary search string for all the fields below
     * @param last          last name field of a user
     * @param first         first name field of a user
     * @param email         email field of a user
     * @param emailVerified emailVerified field of a user
     * @param username      username field of a user
     * @param enabled       Boolean representing if user is enabled or not
     * @param idpAlias The alias of an Identity Provider linked to the user. Parameter supported since Keycloak server 26.4.0
     * @param idpUserId The userId at an Identity Provider linked to the user. Parameter supported since Keycloak server 26.4.0
     * @return number of users matching the given filters
     */
    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Integer count(@QueryParam("search") String search,
                  @QueryParam("lastName") String last,
                  @QueryParam("firstName") String first,
                  @QueryParam("email") String email,
                  @QueryParam("emailVerified") Boolean emailVerified,
                  @QueryParam("username") String username,
                  @QueryParam("enabled") Boolean enabled,
                  @QueryParam("idpAlias") String idpAlias,
                  @QueryParam("idpUserId") String idpUserId,
                  @QueryParam("q") String searchQuery);

    /**
     * Returns the number of users with the given status for emailVerified.
     * If none of the filters is specified this is equivalent to {{@link #count()}}.
     *
     * @param emailVerified emailVerified field of a user
     * @return number of users matching the given filters
     */
    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Integer countEmailVerified(@QueryParam("emailVerified") Boolean emailVerified);

    @Path("{id}")
    UserResource get(@PathParam("id") String id);

    @Path("{id}")
    @DELETE
    Response delete(@PathParam("id") String id);

    @Path("profile")
    UserProfileResource userProfile();

}
