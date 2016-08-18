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
package org.keycloak.storage.user;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserQueryProvider {

    int getUsersCount(RealmModel realm);

    List<UserModel> getUsers(RealmModel realm);
    List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults);

    /**
     * Search for users with username, email or first + last name that is like search string.
     *
     * If possible, implementations should treat the parameter values as partial match patterns i.e. in RDMBS terms use LIKE.
     *
     * This method is used by the admin console search box
     *
     * @param search
     * @param realm
     * @return
     */
    List<UserModel> searchForUser(String search, RealmModel realm);

    /**
     * Search for users with username, email or first + last name that is like search string.
     *
     * If possible, implementations should treat the parameter values as partial match patterns i.e. in RDMBS terms use LIKE.
     *
     * This method is used by the admin console search box
     *
     * @param search
     * @param realm
     * @param firstResult
     * @param maxResults
     * @return
     */
    List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults);

    /**
     * Search for user by parameter.  Valid parameters are:
     * "first" - first name
     * "last" - last name
     * "email" - email
     * "username" - username
     *
     * If possible, implementations should treat the parameter values as partial match patterns i.e. in RDMBS terms use LIKE.
     *
     * This method is used by the REST API when querying users.
     *
     *
     * @param params
     * @param realm
     * @return
     */
    List<UserModel> searchForUser(Map<String, String> params, RealmModel realm);

    /**
     * Search for user by parameter.  Valid parameters are:
     * "first" - first name
     * "last" - last name
     * "email" - email
     * "username" - username
     *
     * If possible, implementations should treat the parameter values as patterns i.e. in RDMBS terms use LIKE.
     * This method is used by the REST API when querying users.
     *
     *
     * @param params
     * @param realm
     * @param firstResult
     * @param maxResults
     * @return
     */
    List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults);

    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults);
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group);


    // Searching by UserModel.attribute (not property)
    List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm);
}
