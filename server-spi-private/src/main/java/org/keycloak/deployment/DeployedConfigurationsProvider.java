/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.deployment;

import java.util.stream.Stream;

import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.provider.Provider;

/**
 * Allows to register "deployed configurations", which are retrieved in runtime from deployed providers and hence are not saved in the DB
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface DeployedConfigurationsProvider extends Provider {

    void registerDeployedAuthenticatorConfig(AuthenticatorConfigModel model);

    Stream<AuthenticatorConfigModel> getDeployedAuthenticatorConfigs();

}
