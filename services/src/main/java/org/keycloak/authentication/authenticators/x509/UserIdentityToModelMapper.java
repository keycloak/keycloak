/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.authentication.authenticators.x509;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:pnalyvayko@agi.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 7/30/2016
 */

public abstract class UserIdentityToModelMapper {

    public abstract UserModel find(AuthenticationFlowContext context, Object userIdentity) throws Exception;

    static class UsernameOrEmailMapper extends UserIdentityToModelMapper {

        @Override
        public UserModel find(AuthenticationFlowContext context, Object userIdentity) throws Exception {
            return KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), userIdentity.toString().trim());
        }
    }

    static class UserIdentityToCustomAttributeMapper extends UserIdentityToModelMapper {
        private List<String> _customAttributes;
        UserIdentityToCustomAttributeMapper(String customAttributes) {
            _customAttributes = Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(customAttributes));
        }

        @Override
        public UserModel find(AuthenticationFlowContext context, Object userIdentity) throws Exception {
            KeycloakSession session = context.getSession();
            List<String> userIdentityValues = Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(userIdentity.toString()));

            if (_customAttributes.isEmpty() || userIdentityValues.isEmpty() || (_customAttributes.size() != userIdentityValues.size())) {
                return null;
            }
            Stream<UserModel> usersStream = session.users().searchForUserByUserAttributeStream(context.getRealm(), _customAttributes.get(0), userIdentityValues.get(0));
            
            for (int i = 1; i <_customAttributes.size(); ++i) {
                String customAttribute = _customAttributes.get(i);
                String userIdentityValue = userIdentityValues.get(i);
                usersStream = usersStream.filter(user -> Objects.equals(user.getFirstAttribute(customAttribute), userIdentityValue));
            }
            List<UserModel> users = usersStream.collect(Collectors.toList());
            if (users.size() > 1) {
                throw new ModelDuplicateException();
            }
            return users.size() == 1 ? users.get(0) : null;
        }
    }

    public static UserIdentityToModelMapper getUsernameOrEmailMapper() {
        return new UsernameOrEmailMapper();
    }

    public static UserIdentityToModelMapper getUserIdentityToCustomAttributeMapper(String attributeName) {
        return new UserIdentityToCustomAttributeMapper(attributeName);
    }
}
