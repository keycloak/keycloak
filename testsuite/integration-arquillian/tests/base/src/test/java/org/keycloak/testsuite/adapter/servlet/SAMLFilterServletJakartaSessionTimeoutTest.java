/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.annotation.UseServletFilter;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

@AppServerContainer(ContainerConstants.APP_SERVER_EAP8)
@UseServletFilter(filterName = "saml-filter", filterClass = "org.keycloak.adapters.saml.servlet.SamlFilter",
        filterDependency = "org.keycloak:keycloak-saml-jakarta-servlet-filter-adapter")
public class SAMLFilterServletJakartaSessionTimeoutTest extends SAMLFilterServletSessionTimeoutTest {
}
