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
import org.keycloak.services.clientpolicy.ClientPolicyLogger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractClientCondition implements ClientPolicyConditionProvider {

    protected static final Logger logger = Logger.getLogger(AbstractClientCondition.class);
    protected static final String LOGMSG_PREFIX = "CLIENT-POLICY";
    protected String logMsgPrefix() {
        return LOGMSG_PREFIX + "@" + session.hashCode() + " :: CONDITION";
    }

    protected final KeycloakSession session;

    public AbstractClientCondition(KeycloakSession session) {
        this.session = session;
    }

    abstract protected <T extends Configuration> T getConfiguration(Class<T> clazz);

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration {
        @JsonProperty("is-negative-login")
        protected Boolean negativeLogic;

        public Boolean isNegativeLogic() {
            return negativeLogic;
        }

        public void setNegativeLogic(Boolean negative) {
            this.negativeLogic = negative;
        }
    }

    @Override
    public boolean isNegativeLogic() {
        return Optional.ofNullable(getConfiguration(Configuration.class).isNegativeLogic()).orElse(Boolean.FALSE).booleanValue();
    }

    protected <T extends Configuration> T getConvertedConfiguration(Object config, Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.convertValue(config, clazz);
        } catch (IllegalArgumentException iae) {
            ClientPolicyLogger.logv(logger, "{0} :: failed for Configuration Setup :: error = {1}", logMsgPrefix(), iae.getMessage());
            return null;
        }
    }

}
