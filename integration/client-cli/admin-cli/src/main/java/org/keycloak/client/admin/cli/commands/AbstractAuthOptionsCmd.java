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
package org.keycloak.client.admin.cli.commands;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.cli.common.BaseAuthOptionsCmd;
import org.keycloak.client.cli.config.ConfigData;

import picocli.CommandLine.Option;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractAuthOptionsCmd extends BaseAuthOptionsCmd implements GlobalOptionsCmdHelper {

    @Option(names = {"-a", "--admin-root"}, description = "URL of Admin REST endpoint root if not default - e.g. http://localhost:8080/admin")
    String adminRestRoot;

    @Option(names = {"-r", "--target-realm"}, description = "Realm to target - when it's different than the realm we authenticate against")
    String targetRealm;

    @Option(names = "--token", description = "Token to use for invocations.  With this option set, every other authentication option is ignored")
    public void setToken(String token) {
        this.externalToken = token;
    }

    public AbstractAuthOptionsCmd() {
        super(KcAdmMain.COMMAND_STATE);
    }

    protected String getTargetRealm(ConfigData config) {
        return targetRealm != null ? targetRealm : config.getRealm();
    }

}
