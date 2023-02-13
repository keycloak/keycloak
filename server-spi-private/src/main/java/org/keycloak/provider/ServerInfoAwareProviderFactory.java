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

package org.keycloak.provider;

import java.util.Map;

/**
 * Marker interface for {@link ProviderFactory} of Provider which wants to show some info on "Server Info" page in Admin console.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface ServerInfoAwareProviderFactory {

    /**
     * Return actual info about the provider. This info contains informations about providers configuration and operational conditions (eg. errors in connection to remote systems etc) which is
     * shown on "Server Info" page then.
     * 
     * @return Map with keys describing value and relevant values itself
     */
    Map<String, String> getOperationalInfo();

}
