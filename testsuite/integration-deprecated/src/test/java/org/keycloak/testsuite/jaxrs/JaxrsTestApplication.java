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

package org.keycloak.testsuite.jaxrs;

import org.keycloak.jaxrs.JaxrsBearerTokenFilterImpl;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JaxrsTestApplication extends Application {

    protected Set<Class<?>> classes = new HashSet<Class<?>>();
    protected Set<Object> singletons = new HashSet<Object>();

    public JaxrsTestApplication(@Context ServletContext context) throws Exception {
        singletons.add(new JaxrsTestResource());

        String configFile = context.getInitParameter(JaxrsFilterTest.CONFIG_FILE_INIT_PARAM);
        JaxrsBearerTokenFilterImpl filter = new JaxrsBearerTokenFilterImpl();
        filter.setKeycloakConfigFile(configFile);
        singletons.add(filter);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
