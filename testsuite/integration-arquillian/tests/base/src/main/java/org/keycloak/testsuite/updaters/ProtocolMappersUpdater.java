/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.updaters;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

/**
 * Updater for mappers. See {@link ServerResourceUpdater} for further details.
 * @author hmlnarik
 */
public class ProtocolMappersUpdater extends ServerResourceUpdater<ProtocolMappersUpdater, ProtocolMappersResource, List<ProtocolMapperRepresentation>> {

    public ProtocolMappersUpdater(ProtocolMappersResource resource) {
        super(resource, resource::getMappers, null);
        this.updater = this::update;
    }

    public ProtocolMappersUpdater add(ProtocolMapperRepresentation... representation) {
        rep.addAll(Arrays.asList(representation));
        return this;
    }

    public ProtocolMappersUpdater clear() {
        rep.clear();
        return this;
    }

    public ProtocolMappersUpdater remove(ProtocolMapperRepresentation representation) {
        rep.remove(representation);
        return this;
    }

    public ProtocolMappersUpdater removeById(String id) {
        for (Iterator<ProtocolMapperRepresentation> it = rep.iterator(); it.hasNext();) {
            ProtocolMapperRepresentation mapper = it.next();
            if (id.equals(mapper.getId())) {
                it.remove();
                break;
            }
        }
        return this;
    }

    private void update(List<ProtocolMapperRepresentation> expectedMappers) {
        List<ProtocolMapperRepresentation> currentMappers = resource.getMappers();

        Set<String> currentMapperIds = currentMappers.stream().map(ProtocolMapperRepresentation::getId).collect(Collectors.toSet());
        Set<String> expectedMapperIds = expectedMappers.stream().map(ProtocolMapperRepresentation::getId).collect(Collectors.toSet());

        List<ProtocolMapperRepresentation> toAdd = expectedMappers.stream()
          .filter(mapper -> ! currentMapperIds.contains(mapper.getId()))
          .collect(Collectors.toList());
        Stream<ProtocolMapperRepresentation> toRemove = currentMappers.stream()
          .filter(mapper -> ! expectedMapperIds.contains(mapper.getId()));

        resource.createMapper(toAdd);
        toRemove.map(ProtocolMapperRepresentation::getId).forEach(resource::delete);
    }

}
