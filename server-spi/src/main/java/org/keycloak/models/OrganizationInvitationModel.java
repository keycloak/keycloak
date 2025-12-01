/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models;

import org.keycloak.common.util.Time;

/**
 * Model representing an organization invitation.
 */
public interface OrganizationInvitationModel {

    enum Filter {
        STATUS,
        FIRST_NAME,
        LAST_NAME,
        EMAIL,
        SEARCH
    }

    /**
     * Status of an organization invitation.
     */
    enum InvitationStatus {
        PENDING, EXPIRED
    }

    /**
     * Returns the unique identifier of this invitation.
     *
     * @return the unique identifier
     */
    String getId();

    /**
     * Returns the organization ID this invitation belongs to.
     *
     * @return the organization ID
     */
    String getOrganizationId();

    /**
     * Returns the email address of the invited user.
     *
     * @return the email address
     */
    String getEmail();

    /**
     * Sets the email address of the invited user.
     *
     * @param email the email address
     */
    void setEmail(String email);

    /**
     * Returns the first name of the invited user.
     *
     * @return the first name
     */
    String getFirstName();

    /**
     * Sets the first name of the invited user.
     *
     * @param firstName the first name
     */
    void setFirstName(String firstName);

    /**
     * Returns the last name of the invited user.
     *
     * @return the last name
     */
    String getLastName();

    /**
     * Sets the last name of the invited user.
     *
     * @param lastName the last name
     */
    void setLastName(String lastName);

    /**
     * Returns the timestamp when this invitation was created.
     *
     * @return the creation timestamp
     */
    int getCreatedAt();

    /**
     * Returns the timestamp when this invitation expires.
     *
     * @return the expiration timestamp, or null if no expiration
     */
    int getExpiresAt();

    /**
     * Sets the timestamp when this invitation expires.
     *
     * @param expiresAt the expiration timestamp
     */
    void setExpiresAt(int expiresAt);

    /**
     * Returns the invitation link.
     *
     * @return the invitation link
     */
    String getInviteLink();

    /**
     * Sets the invitation link.
     *
     * @param inviteLink the invitation link
     */
    void setInviteLink(String inviteLink);

    /**
     * Returns the current status of this invitation.
     *
     * @return the invitation status
     */
    InvitationStatus getStatus();

    /**
     * Returns whether this invitation has expired.
     *
     * @return true if expired, false otherwise
     */
    default boolean isExpired() {
        return Time.currentTime() > getExpiresAt();
    }
}
