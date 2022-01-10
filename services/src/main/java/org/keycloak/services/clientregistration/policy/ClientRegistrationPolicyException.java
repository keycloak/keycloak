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

package org.keycloak.services.clientregistration.policy;

import org.keycloak.component.ComponentModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationPolicyException extends RuntimeException {

    private ComponentModel policyModel;

    public ClientRegistrationPolicyException(String message) {
        super(message);
    }

    public ClientRegistrationPolicyException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ComponentModel getPolicyModel() {
        return policyModel;
    }

    public void setPolicyModel(ComponentModel policyModel) {
        this.policyModel = policyModel;
    }

    @Override
    public String getMessage() {
        return policyModel==null ? super.getMessage() : String.format("Policy '%s' rejected request to client-registration service. Details: %s", policyModel.getName(), super.getMessage());
    }
}
