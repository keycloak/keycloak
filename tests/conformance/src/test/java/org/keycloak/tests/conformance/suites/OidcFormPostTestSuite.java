/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.conformance.suites;

import org.keycloak.tests.conformance.oidc.OidcFormPostBasicTest;
import org.keycloak.tests.conformance.oidc.OidcFormPostHybridTest;
import org.keycloak.tests.conformance.oidc.OidcFormPostImplicitTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * The OpenID Connect Core Form Post Basic, Implicit and Hybrid OP certification plans.
 */
@Suite
@SelectClasses({OidcFormPostBasicTest.class, OidcFormPostImplicitTest.class, OidcFormPostHybridTest.class})
public class OidcFormPostTestSuite {
}
