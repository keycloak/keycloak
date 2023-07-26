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
 */

package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.processing.event.source.filter.OnUpdateFilter;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * addresses the additional events noted on https://github.com/fabric8io/kubernetes-client/issues/5215
 * <p>
 * - usage of secret stringData has been removed,
 * - but ingress usage of empty strings is still problematic
 * it seems best to leave this in place for all resources to make sure we don't get in a reconciliation loop.
 * <p>
 * This should be removable after switching to dependent resources
 *
 */
public class MetadataAwareOnUpdateFilter<T extends HasMetadata> implements OnUpdateFilter<T> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean accept(HasMetadata newResource, HasMetadata oldResource) {
        ObjectMeta newMetadata = newResource.getMetadata();
        ObjectMeta oldMetadata = oldResource.getMetadata();
        // quick check if anything meaningful has changed
        if (!Objects.equals(newMetadata.getAnnotations(), oldMetadata.getAnnotations())
                || !Objects.equals(newMetadata.getLabels(), oldMetadata.getLabels())
                || !Objects.equals(newMetadata.getGeneration(), oldMetadata.getGeneration())) {
            return true;
        }
        // check everything else besides the metadata
        // since the hierarchy of model the does not implement hasCode/equals, we'll convert to a generic form
        // that should be less expensive than full serialization
        var newMap = (ObjectNode)mapper.valueToTree(newResource);
        newMap.remove("metadata");
        var oldMap = (ObjectNode)mapper.valueToTree(oldResource);
        oldMap.remove("metadata");
        return !oldMap.equals(newMap);
    }

}
