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

    /**
     * Mark this validation as sucessful
     *
     */
    void success();
}
