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

package org.keycloak.services.clientpolicy.context;

import org.keycloak.protocol.oidc.endpoints.UserInfoEndpoint;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class UserInfoRequestContext implements ClientPolicyContext {

    private UserInfoEndpoint.TokenForUserInfo tokenForUserInfo;

    public UserInfoRequestContext(UserInfoEndpoint.TokenForUserInfo tokenForUserInfo) {
        this.tokenForUserInfo = tokenForUserInfo;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.USERINFO_REQUEST;
    }

    public UserInfoEndpoint.TokenForUserInfo getTokenForUserInfo() {
        return tokenForUserInfo;
    }

}
