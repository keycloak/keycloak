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

package org.keycloak.testsuite.domainextension;

import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.client.resources.TestExampleCompanyResource;
import org.keycloak.testsuite.util.RealmBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(value = {AuthServer.REMOTE})
// This is testing custom SPI which is, in case of remote server, deployed on container as part of testsuite providers.
// It looks like the problem is, that in the time of loading spis during keycloak deployment, the deployment of Testsuite providers
// is not processed yet, hence the spi is not present yet, which results in nullpointer exception because service provided by the spi
// is not loaded
public class CustomExtensionTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation foo = RealmBuilder.create().name("foo").build();
        testRealms.add(foo);
    }

    @BeforeClass
    public static void checkNotMapStorage() {
        ProfileAssume.assumeFeatureDisabled(Profile.Feature.MAP_STORAGE);
    }

    @Test
    public void testDomainExtension() throws Exception {
        companyResource().createCompany("foo", buildCompany("foo-company"));
        companyResource().createCompany("foo", buildCompany("bar-company"));
        companyResource().createCompany("master", buildCompany("master-company"));

        List<CompanyRepresentation> fooCompanies = companyResource().getCompanies("foo");
        List<CompanyRepresentation> masterCompanies = companyResource().getCompanies("master");

        assertCompanyNames(fooCompanies, "foo-company", "bar-company");
        assertCompanyNames(masterCompanies, "master-company");

        companyResource().deleteAllCompanies("foo");
        companyResource().deleteAllCompanies("master");
    }

    private TestExampleCompanyResource companyResource() {
        return testingClient.testExampleCompany();
    }

    private CompanyRepresentation buildCompany(String companyName) {
        CompanyRepresentation rep = new CompanyRepresentation();
        rep.setName(companyName);
        return rep;
    }

    private void assertCompanyNames(List<CompanyRepresentation> companies, String... expectedNames) {
        Set<String> names = new HashSet<>();
        for (CompanyRepresentation comp : companies) {
            names.add(comp.getName());
        }

        Assert.assertEquals(expectedNames.length, names.size());
        for (String expectedName : expectedNames) {
            Assert.assertTrue(names.contains(expectedName));
        }
    }
}
