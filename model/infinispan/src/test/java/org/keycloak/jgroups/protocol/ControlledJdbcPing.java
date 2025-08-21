/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups.protocol;

import java.util.List;

import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.protocols.PingData;

/**
 * A controllable KEYCLOAK_JDBC_PING2 where we can overwrite the view, the data returned from the database, and simulate exceptions.
 */
public class ControlledJdbcPing extends KEYCLOAK_JDBC_PING2 {

    private volatile List<PingData> pingData = List.of();
    private volatile Exception exception;

    @Override
    protected List<PingData> readFromDB(String cluster) throws Exception {
        if (exception != null) {
            throw exception;
        }
        return pingData;
    }

    public void setPingData(List<Address> coordinators) {
        this.pingData = coordinators.stream()
                .map(address -> new PingData(address, true).coord(true))
                .toList();
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public void setView(Address coordinator) {
        // view id is irrelevant
        this.view = View.create(coordinator, 1, coordinator);
    }
}
