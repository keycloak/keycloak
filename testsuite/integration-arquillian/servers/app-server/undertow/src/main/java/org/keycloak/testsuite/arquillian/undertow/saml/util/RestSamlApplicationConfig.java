/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.arquillian.undertow.saml.util;

import java.util.Set;
import javax.ws.rs.core.Application;

/**
 * Wildfly JAX-RS Integration has support for scanning  deployment for annotations.
 * 
 * https://github.com/wildfly/wildfly/blob/14.0.1.Final/jaxrs/src/main/java/org/jboss/as/jaxrs/deployment/JaxrsAnnotationProcessor.java
 * 
 * On undertow we have to set Application Class manually:
 * 
 * ResteasyDeployment deployment = new ResteasyDeployment();
 * deployment.setApplication(application);
 * 
 * @author vramik
 */
public class RestSamlApplicationConfig extends Application {

    private final Set<Class<?>> classes;

    public RestSamlApplicationConfig(Set<Class<?>> classes) {
        this.classes = classes;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
