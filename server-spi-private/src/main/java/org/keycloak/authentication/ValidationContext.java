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

import org.keycloak.models.utils.FormMessage;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

/**
 * Interface that encapsulates the current validation that is being performed.  Calling success() or validationError()
 * sets the status of this current validation.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ValidationContext extends FormContext {
    /**
     * Mark this validation as having a validation error
     *
     * @param formData form data you want to display when the form is refreshed
     * @param errors error messages to display on the form
     */
    void validationError(MultivaluedMap<String, String> formData, List<FormMessage> errors);

    void error(String error);

    /**
     * Mark this validation as sucessful
     *
     */
    void success();

    /**
     * The error messages of this current validation will take precedence over any others. Other error messages will not
     * be shown. This is useful to prevent validation from leaking to an attacker. For example, the recaptcha validator
     * calls this method so that usernames cannot be phished
     */
    void excludeOtherErrors();
}
