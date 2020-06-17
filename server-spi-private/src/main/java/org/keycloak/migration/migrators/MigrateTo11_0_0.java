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

package org.keycloak.migration.migrators;

import java.util.Objects;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.KeycloakSession;

public class MigrateTo11_0_0 implements Migration {

  public static final ModelVersion VERSION = new ModelVersion("11.0.0");

  @Override
  public void migrate(KeycloakSession session) {
    session.realms().getRealms().stream()
                                .map(realm -> realm.getClientByClientId("delete_account"))
                                .filter(client -> Objects.isNull(client.getRole(AccountRoles.DELETE_ACCOUNT)))
                                .forEach(client -> client.addRole(AccountRoles.DELETE_ACCOUNT).setDescription("${"+AccountRoles.DELETE_ACCOUNT+"}"));
  }

  @Override
  public ModelVersion getVersion() {
    return VERSION;
  }
}
