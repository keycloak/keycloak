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
import org.keycloak.provider.Provider;

import javax.ws.rs.core.Response;

/**
 * This class is responsible for rendering a form.  The way it works is that each FormAction that is a child of this
 * FormAuthenticator, will have its buildPage() method call first, then the FormAuthenticator.render() method will be invoked.
 *
 * This gives each FormAction a chance to add information to the form in an independent manner.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormAuthenticator extends Provider {
    /**
     * Called to render the FormAuthenticator's challenge page.  If null is returned, then success is assumed and the
     * next authenticator in the flow will be invoked.
     *
     * @param context
     * @param form
     * @return
     */
    Response render(FormContext context, LoginFormsProvider form);
}
