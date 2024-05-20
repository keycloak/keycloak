/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.organization.admin;

import org.keycloak.common.Profile;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.exportimport.ExportImportTest;

/**
 * Tests the export/import functionality with the organization feature enabled.
 *
 * NOTE: When export/import of organizations is implemented and the organization feature is supported, we should either enhance
 * this class or the existing ExportImportTest to check org-specific settings.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@EnableFeature(Profile.Feature.ORGANIZATION)
public class OrganizationEnabledExportImportTest extends ExportImportTest {
}
