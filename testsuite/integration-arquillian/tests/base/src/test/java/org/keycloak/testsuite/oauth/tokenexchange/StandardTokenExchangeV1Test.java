/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oauth.tokenexchange;

import org.keycloak.common.Profile;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

/**
 * Tests for standard token exchange (internal-internal) and token-exchange-v1
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
public class StandardTokenExchangeV1Test extends AbstractStandardTokenExchangeTest {
}
