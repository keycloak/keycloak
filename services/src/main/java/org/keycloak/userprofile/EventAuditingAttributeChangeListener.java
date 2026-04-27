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
package org.keycloak.userprofile;

import java.util.List;

import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.UserModel;

/**
 * {@link AttributeChangeListener} to audit user profile attribute changes into {@link Event}.
 *
 * Adds info about user profile attribute change into {@link Event}'s detail field.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 * @see UserProfile#update(AttributeChangeListener...)
 */
public class EventAuditingAttributeChangeListener implements AttributeChangeListener {

    private EventBuilder event;
    private UserProfile profile;

    /**
     * @param profile used to read attribute configuration from
     * @param event to add detail info into
     */
    public EventAuditingAttributeChangeListener(UserProfile profile, EventBuilder event) {
        super();
        this.profile = profile;
        this.event = event;
    }

    @Override
    public void onChange(String attributeName, UserModel userModel, List<String> oldValue) {
        if (attributeName.equals(UserModel.FIRST_NAME)) {
            event.detail(Details.PREVIOUS_FIRST_NAME, oldValue).detail(Details.UPDATED_FIRST_NAME, userModel.getFirstName());
        } else if (attributeName.equals(UserModel.LAST_NAME)) {
            event.detail(Details.PREVIOUS_LAST_NAME, oldValue).detail(Details.UPDATED_LAST_NAME, userModel.getLastName());
        } else if (attributeName.equals(UserModel.EMAIL)) {
            event.detail(Details.PREVIOUS_EMAIL, oldValue).detail(Details.UPDATED_EMAIL, userModel.getEmail());
        } else {
            event.detail(Details.PREF_PREVIOUS + attributeName, oldValue).detail(Details.PREF_UPDATED + attributeName, userModel.getAttributeStream(attributeName));
        }
    }

}
