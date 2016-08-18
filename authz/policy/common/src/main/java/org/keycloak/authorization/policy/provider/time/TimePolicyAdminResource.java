/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.policy.provider.time;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;

import java.text.SimpleDateFormat;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyAdminResource implements PolicyProviderAdminService {

    @Override
    public void onCreate(Policy policy) {
        validateConfig(policy);
    }

    private void validateConfig(Policy policy) {
        String nbf = policy.getConfig().get("nbf");
        String noa = policy.getConfig().get("noa");

        if (nbf == null && noa == null) {
            throw new RuntimeException("You must provide NotBefore, NotOnOrAfter or both.");
        }

        validateFormat(nbf);
        validateFormat(noa);
    }

    @Override
    public void onUpdate(Policy policy) {
        validateConfig(policy);
    }

    @Override
    public void onRemove(Policy policy) {
    }

    private void validateFormat(String date) {
        try {
            new SimpleDateFormat(TimePolicyProvider.DEFAULT_DATE_PATTERN).parse(TimePolicyProvider.format(date));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse a date using format [" + date + "]");
        }
    }
}
