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

package org.keycloak.testsuite.arquillian.provider;

import java.lang.annotation.Annotation;

import org.keycloak.testsuite.arquillian.TestContext;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 *
 * @author tkyjovsk
 */
public class TestContextProvider implements ResourceProvider {

    @Inject
    Instance<TestContext> testContext;

    @Override
    public boolean canProvide(Class<?> type) {
        return TestContext.class.isAssignableFrom(type);
    }

    @Override
    @ClassInjection
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return testContext.get();
    }

}
