/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.attributeprocessing;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.Attributes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;

/**
 * UserAttributeProcessor which sets a timestamp on when an attribute is updated.
 * To use the processor, configure an attribute with the annotation "timestamp" and a pattern acceptable for "SimpleDateFormat".
 * If the user attribute was "attribute", the timestamp will be saved in the attribute "attribute.timestamp".
 *
 * @author <a href="mailto:external.Martin.Idel@bosch.io">Martin Idel</a>
 */
public class UpdateTimestampAttributeProcessor extends AbstractUserAttributeProcessor<UserAttributeProcessorProvider> {
    public static final String ID = "update-timestamp-processor";
    private final static Logger LOG = Logger.getLogger(UpdateTimestampAttributeProcessor.class);
    private static final String ANNOTATION = "timestamp";

    @Override
    public BiConsumer<String, UserModel> createListener(Attributes attributes) {
        return (name, user) -> {
            if (attributes != null
                    && attributes.getMetadata(name) != null
                    && attributes.getMetadata(name).getAnnotations() != null
                    && attributes.getMetadata(name).getAnnotations().containsKey(ANNOTATION))
            {
                Object timestampPattern = attributes.getMetadata(name).getAnnotations().get(ANNOTATION);
                try {
                    Date now = new Date(System.currentTimeMillis());
                    String timestamp = new SimpleDateFormat(timestampPattern.toString()).format(now);
                    user.setSingleAttribute(name + ".timestamp", timestamp);
                } catch (IllegalArgumentException | NullPointerException e) {
                    LOG.errorf("Timestamp pattern %s given in annotation %s could not be parsed. Original error: %s", timestampPattern, ANNOTATION, e.getMessage());
                }
            }
        };
    }

    @Override
    public UserAttributeProcessorProvider create(KeycloakSession session) {
        return new UpdateTimestampAttributeProcessor();
    }

    @Override
    public String getId() {
        return ID;
    }
}
