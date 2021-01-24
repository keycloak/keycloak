/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.condition;

import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class AnyClientCondition extends AbstractClientCondition {

    private static final Logger logger = Logger.getLogger(AnyClientCondition.class);

    // to avoid null configuration, use vacant new instance to indicate that there is no configuration set up.
    private Configuration configuration = new Configuration();

    public AnyClientCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    protected <T extends AbstractClientCondition.Configuration> T getConfiguration(Class<T> clazz) {
        return (T) configuration;
    }
 
    @Override
    public void setupConfiguration(Object config) {
        // to avoid null configuration, use vacant new instance to indicate that there is no configuration set up.
        configuration = Optional.ofNullable(getConvertedConfiguration(config, Configuration.class)).orElse(new Configuration());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration extends AbstractClientCondition.Configuration {
    }

    @Override
    public String getProviderId() {
        return AnyClientConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        return ClientPolicyVote.YES;
    }

}
