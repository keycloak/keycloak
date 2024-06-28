package org.keycloak.testsuite.util.javascript;

import java.io.Serializable;
import java.util.Map;

/**
 * @author mhajas
 */
public interface ResponseValidator extends Serializable {

    void validate(Map<String, Object> response);
}
