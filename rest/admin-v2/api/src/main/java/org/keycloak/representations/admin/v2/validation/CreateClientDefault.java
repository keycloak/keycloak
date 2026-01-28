package org.keycloak.representations.admin.v2.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

@GroupSequence({CreateClient.class, Default.class})
// Jakarta Validation Group - validation is done only when creating a client + default group included
public interface CreateClientDefault {
}
