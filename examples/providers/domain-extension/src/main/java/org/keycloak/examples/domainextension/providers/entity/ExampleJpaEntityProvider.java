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

package org.keycloak.examples.domainextension.providers.entity;

import java.util.Arrays;
import java.util.List;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.examples.domainextension.entities.Company;
import org.keycloak.examples.domainextension.entities.Region;
import org.keycloak.examples.domainextension.entities.UserAccount;
import org.keycloak.examples.domainextension.entities.UserAccountRegionRole;

/**
 * @author <a href="mailto:erik.mulder@docdatapayments.com">Erik Mulder</a>
 * 
 * Example JpaEntityProvider.
 */
public class ExampleJpaEntityProvider implements JpaEntityProvider {

    @Override
    public List<Class<?>> getEntities() {
        return Arrays.asList(Company.class, Region.class, UserAccount.class, UserAccountRegionRole.class);
    }

    @Override
    public String getChangelogLocation() {
    	return "example-changelog.xml";
    }
    
    @Override
    public void close() {
    }

}
