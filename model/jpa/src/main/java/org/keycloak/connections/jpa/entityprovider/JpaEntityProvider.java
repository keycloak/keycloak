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

package org.keycloak.connections.jpa.entityprovider;

import java.util.List;

import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:erik.mulder@docdatapayments.com">Erik Mulder</a>
 * 
 * A JPA Entity Provider can supply extra JPA entities that the Keycloak system should include in it's entity manager. The
 * entities should be provided as a list of Class objects.
 */
public interface JpaEntityProvider extends Provider {

    /**
     * Return the entities that should be added to the entity manager.
     * 
     * @return list of class objects
     */
	List<Class<?>> getEntities();
	
	/**
	 * Return the location of the Liquibase changelog that facilitates the extra JPA entities.
	 * This should be a location that can be found on the same classpath as the entity classes.
	 * 
	 * @return a changelog location or null if not needed
	 */
	String getChangelogLocation();

	/**
	 * Return the ID of provider factory, which created this provider. Might be used to "compute" the table name of liquibase changelog table.
	 * @return ID of provider factory
	 */
	String getFactoryId();

}
