/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.subsystem.logging;

import java.util.List;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.vfs.VirtualFile;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * This interface to be fleshed out later when error messages are fully externalized.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
@MessageLogger(projectCode = "KEYCLOAK")
public interface KeycloakLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    KeycloakLogger ROOT_LOGGER = Logger.getMessageLogger(KeycloakLogger.class, "org.jboss.keycloak");

 /*   @LogMessage(level = ERROR)
    @Message(id = 12600, value = "Could not load JSF managed bean class: %s")
    void managedBeanLoadFail(String managedBean);

    @LogMessage(level = ERROR)
    @Message(id = 12601, value = "JSF managed bean class %s has no default constructor")
    void managedBeanNoDefaultConstructor(String managedBean);

    @LogMessage(level = ERROR)
    @Message(id = 12602, value = "Failed to parse %s, managed beans defined in this file will not be available")
    void managedBeansConfigParseFailed(VirtualFile facesConfig);

    @LogMessage(level = WARN)
    @Message(id = 12603, value = "Unknown JSF version '%s'.  Default version '%s' will be used instead.")
    void unknownJSFVersion(String version, String defaultVersion);

    @LogMessage(level = WARN)
    @Message(id = 12604, value = "JSF version slot '%s' is missing from module %s")
    void missingJSFModule(String version, String module);

    @LogMessage(level = INFO)
    @Message(id = 12605, value = "Activated the following JSF Implementations: %s")
    void activatedJSFImplementations(List target); */
}
