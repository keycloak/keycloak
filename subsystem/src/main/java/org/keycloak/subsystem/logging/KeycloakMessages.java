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

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This interface to be fleshed out later when error messages are fully externalized.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
@MessageBundle(projectCode = "TLIP")
public interface KeycloakMessages {

    /**
     * The messages
     */
    KeycloakMessages MESSAGES = Messages.getBundle(KeycloakMessages.class);
/*
    @Message(id = 12650, value = "Failed to load annotated class: %s")
    String classLoadingFailed(DotName clazz);

    @Message(id = 12651, value = "Annotation %s in class %s is only allowed on classes")
    String invalidAnnotationLocation(Object annotation, AnnotationTarget classInfo);

    @Message(id = 12652, value = "Instance creation failed")
    RuntimeException instanceCreationFailed(@Cause Throwable t);

    @Message(id = 12653, value = "Instance destruction failed")
    RuntimeException instanceDestructionFailed(@Cause Throwable t);

    @Message(id = 12654, value = "Thread local injection container not set")
    IllegalStateException noThreadLocalInjectionContainer();

    @Message(id = 12655, value = "@ManagedBean is only allowed at class level %s")
    String invalidManagedBeanAnnotation(AnnotationTarget target);

    @Message(id = 12656, value = "Default JSF implementation slot '%s' is invalid")
    DeploymentUnitProcessingException invalidDefaultJSFImpl(String defaultJsfVersion);
    */
}
