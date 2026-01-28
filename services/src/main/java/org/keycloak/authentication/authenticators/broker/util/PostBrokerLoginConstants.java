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

package org.keycloak.authentication.authenticators.broker.util;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface PostBrokerLoginConstants {

    // ClientSession note with serialized BrokeredIdentityContext used during postBrokerLogin flow
    String PBL_BROKERED_IDENTITY_CONTEXT = "PBL_BROKERED_IDENTITY_CONTEXT";

    // ClientSession note flag specifying if postBrokerLogin flow was triggered after 1st login with this broker after firstBrokerLogin flow is finished (true)
    // or after 2nd or more login with this broker (false)
    String PBL_AFTER_FIRST_BROKER_LOGIN = "PBL_AFTER_FIRST_BROKER_LOGIN";

    // Prefix for the clientSession note key (suffix will be identityProvider alias, so the whole note key will be something like PBL_AUTH_STATE.facebook )
    // It holds the flag whether PostBrokerLogin flow for specified broker was successfully executed for this clientSession
    String PBL_AUTH_STATE_PREFIX = "PBL_AUTH_STATE.";
}
