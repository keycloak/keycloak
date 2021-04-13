/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.grants.ciba.resolvers;

import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * Provides the resolver that converts several types of receives login hint to its corresponding UserModel.
 * Also converts between UserModel and the user identifier that can be recognized by the external entity executing AuthN and AuthZ by AD.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public interface CIBALoginUserResolver extends Provider {

    /**
     * This method receives the login_hint parameter and returns its corresponding UserModel.
     *
     * @param loginHint
     * @return UserModel
     */
    default UserModel getUserFromLoginHint(String loginHint) {
        return null;
    }

    /**
     * This method receives the login_hint_token parameter and returns its corresponding UserModel.
     *
     * @param loginHintToken
     * @return UserModel
     */
    default UserModel getUserFromLoginHintToken(String loginHintToken) {
        return null;
    }

    /**
     * This method receives the id_token_hint parameter and returns its corresponding UserModel.
     *
     * @param idToken
     * @return UserModel
     */
    default UserModel getUserFromIdTokenHint(String idToken) {
        return null;
    }

    /**
     * This method converts the UserModel to its corresponding user identifier that can be recognized by the external entity executing AuthN and AuthZ by AD.
     *
     * @param user
     * @return its corresponding user identifier
     */
    default String getInfoUsedByAuthentication(UserModel user) {
        return user.getUsername();
    }

    /**
     * This method converts the user identifier that can be recognized by the external entity executing AuthN and AuthZ by AD to the corresponding UserModel.
     *
     * @param info
     * @return UserModel
     */
    UserModel getUserFromInfoUsedByAuthentication(String info);

}
