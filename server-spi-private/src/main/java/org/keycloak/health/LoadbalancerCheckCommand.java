/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.health;

import org.keycloak.provider.ProviderEvent;

/**
 * Providers might listen for this command and update the status to down.
 * Once one provider marks it as down, the status down will be returned to loadbalancer.
 *
 * @author <a href="mailto:aschwart@redhat.com">Alexander Schwartz</a>
 */
public class LoadbalancerCheckCommand implements ProviderEvent {

    boolean down = false;

    public void down() {
        down = true;
    }

    public boolean isDown() {
        return down;
    }
}
