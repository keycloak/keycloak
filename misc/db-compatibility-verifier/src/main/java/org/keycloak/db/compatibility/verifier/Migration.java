package org.keycloak.db.compatibility.verifier;

import com.fasterxml.jackson.annotation.JsonProperty;

record Migration(@JsonProperty("class") String clazz) {
}
