package org.keycloak.scim.resource.user;

import org.keycloak.scim.resource.common.MultiValuedAttribute;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupMembership extends MultiValuedAttribute {

}
