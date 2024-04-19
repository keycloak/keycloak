/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.clienttype.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.delegate.ClientModelLazyDelegate;
import org.keycloak.client.clienttype.ClientType;
import org.keycloak.client.clienttype.ClientTypeException;

/**
 * Delegates to client-type and underlying delegate
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TypeAwareClientModelDelegate extends ClientModelLazyDelegate {

    private final ClientType clientType;

    public TypeAwareClientModelDelegate(ClientType clientType, Supplier<ClientModel> clientModelSupplier) {
        super(clientModelSupplier);

        if (clientType == null) {
            throw new IllegalArgumentException("Null client type not supported for client " + getClientId());
        }
        this.clientType = clientType;
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return getBooleanProperty("standardFlowEnabled", super::isStandardFlowEnabled);
    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        setBooleanProperty("standardFlowEnabled", standardFlowEnabled, super::setStandardFlowEnabled);
    }


    protected boolean getBooleanProperty(String propertyName, Supplier<Boolean> clientGetter) {
        // Check if clientType supports the feature. If not, simply return false
        if (!clientType.isApplicable(propertyName)) {
            return false;
        }

        //  Check if this is read-only. If yes, then we just directly delegate to return stuff from the clientType rather than from client
        if (clientType.isReadOnly(propertyName)) {
            return clientType.getDefaultValue(propertyName, Boolean.class);
        }

        // Delegate to clientGetter
        return clientGetter.get();
    }

    protected void setBooleanProperty(String propertyName, Boolean newValue, Consumer<Boolean> clientSetter) {
        // Check if clientType supports the feature. If not, return directly
        if (!clientType.isApplicable(propertyName)) {
            return;
        }

        // Check if this is read-only. If yes and there is an attempt to change some stuff, then throw an exception
        if (clientType.isReadOnly(propertyName)) {
            Boolean oldVal = clientType.getDefaultValue(propertyName, Boolean.class);
            if (!ObjectUtil.isEqualOrBothNull(oldVal, newValue)) {
                throw new ClientTypeException("Property " + propertyName + " of client " + getClientId() + " is read-only due to client type " + clientType.getName());
            }
        }

        // Call clientSetter
        clientSetter.accept(newValue);
    }
}