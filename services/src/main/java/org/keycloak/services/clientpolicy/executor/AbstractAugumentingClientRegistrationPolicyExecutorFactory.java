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

package org.keycloak.services.clientpolicy.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

public abstract class AbstractAugumentingClientRegistrationPolicyExecutorFactory implements ClientPolicyExecutorProviderFactory  {

    protected static final String IS_AUGMENT = "is-augment";

    private static final ProviderConfigProperty IS_AUGMENT_PROPERTY = new ProviderConfigProperty(
            IS_AUGMENT, null, null, ProviderConfigProperty.BOOLEAN_TYPE, false);

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>(Arrays.asList(IS_AUGMENT_PROPERTY));
    }

}
