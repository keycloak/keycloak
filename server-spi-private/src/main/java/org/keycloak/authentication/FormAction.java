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

package org.keycloak.authentication;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * Fine grain processing of a form.  Allows you to split up the processing of a form into smaller parts so that you can
 * enable/disable them from the admin console.  For example, Recaptcha is a FormAction.  This allows you as the admin
 * to turn Recaptcha on/off even though it is on the same form/page as other registration validation.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormAction extends Provider {
    /**
     * When a FormAuthenticator is rendering the challenge page, even FormAction.buildPage() method will be called
     * This gives the FormAction the opportunity to add additional attributes to the form to be displayed.
     *
     * @param context
     * @param form
     */
    void buildPage(FormContext context, LoginFormsProvider form);
    /**
     * This is the first phase of form processing.  Each FormAction.validate() method is called.  This gives the
     * FormAction a chance to validate and challenge if user input is invalid.
     *
     * @param context
     */
    void validate(ValidationContext context);

    /**
     * Called after all validate() calls of all FormAction providers are successful.
     *
     * @param context
     */
    void success(FormContext context);

    /**
     * Does this FormAction require that a user be set? For registration, this method will always return false.
     *
     * @return
     */
    boolean requiresUser();

    /**
     * Is this FormAction configured for the current user?
     *
     * @param session
     * @param realm
     * @param user
     * @return
     */
    boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user);

    /**
     * Set actions to configure authenticator
     *
     */
    void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user);


}
