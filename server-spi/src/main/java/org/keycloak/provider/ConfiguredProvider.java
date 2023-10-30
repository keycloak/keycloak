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

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ConfiguredProvider {

    /**
     * Returns the category for this provider so that it can be used to group it together
     * with others when rendering forms.
     *
     * @return the provider category. Can be {@code null}.
     */
    default String getDisplayCategory() {
        return null;
    }

    /**
     * Returns the user-friendly name for this provider when rendering forms.
     *
     * @return the name for this provider. Defaults to the full-qualified class name.
     */
    default String getDisplayType() {
        return getClass().getName();
    }

    String getHelpText();

    List<ProviderConfigProperty> getConfigProperties();

    /**
     * Returns a default configuration for this provider.
     *
     * @param <C> the type of the configuration
     * @return the default configuration
     */
    default <C> C getConfig() {
        return null;
    }
}
