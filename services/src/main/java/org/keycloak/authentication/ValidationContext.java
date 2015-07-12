package org.keycloak.authentication;

import org.keycloak.models.utils.FormMessage;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public interface ValidationContext extends FormContext {
    void validationError(MultivaluedMap<String, String> formData, List<FormMessage> errors);
    void success();
}
