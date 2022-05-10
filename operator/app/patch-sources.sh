#!/usr/bin/env bash

sedCommand="sed"
if [ "$(uname)" == "Darwin" ]; then
  if hash gsed 2>/dev/null; then
    sedCommand="gsed"
  fi    
fi

function addAnnotation() {
  local match=$1
  local annotation=$2
  local file=$3

  $sedCommand -i "/^.*${match}.*/i ${annotation}" ${file}
}

addAnnotation \
  "protected List<GroupRepresentation> subGroups;" \
  "@io.fabric8.crd.generator.annotation.SchemaFrom(type = org.keycloak.representations.overrides.NoSubGroupsGroupRepresentationList.class)" \
  target/keycloak-core/org/keycloak/representations/idm/GroupRepresentation.java

addAnnotation \
  "private MultivaluedHashMap<String, ComponentExportRepresentation> components;" \
  "@io.fabric8.crd.generator.annotation.SchemaFrom(type = org.keycloak.representations.overrides.ComponentExportRepresentationMap.class)" \
  target/keycloak-core/org/keycloak/representations/idm/RealmRepresentation.java

addAnnotation \
  "private MultivaluedHashMap<String, String> config;" \
  "@io.fabric8.crd.generator.annotation.SchemaFrom(type = org.keycloak.representations.overrides.MultivaluedStringStringHashMap.class)" \
  target/keycloak-core/org/keycloak/representations/idm/CredentialRepresentation.java

addAnnotation \
  "private MultivaluedHashMap<String, String> config;" \
  "@io.fabric8.crd.generator.annotation.SchemaFrom(type = org.keycloak.representations.overrides.MultivaluedStringStringHashMap.class)" \
  target/keycloak-core/org/keycloak/representations/idm/ComponentRepresentation.java

addAnnotation \
  "private MultivaluedHashMap<String, ComponentExportRepresentation> subComponents = new MultivaluedHashMap<>();" \
  "@io.fabric8.crd.generator.annotation.SchemaFrom(type = org.keycloak.representations.overrides.NoSubcomponentsComponentExportRepresentationMap.class)" \
  target/keycloak-core/org/keycloak/representations/idm/ComponentExportRepresentation.java

addAnnotation \
  "private MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();" \
  "@io.fabric8.crd.generator.annotation.SchemaFrom(type = org.keycloak.representations.overrides.MultivaluedStringStringHashMap.class)" \
  target/keycloak-core/org/keycloak/representations/idm/ComponentExportRepresentation.java

addAnnotation \
  "private List<PolicyRepresentation> policies;" \
  "@com.fasterxml.jackson.annotation.JsonIgnore" \
  target/keycloak-core/org/keycloak/representations/idm/authorization/ScopeRepresentation.java

addAnnotation \
  "private List<ResourceRepresentation> resources;" \
  "@com.fasterxml.jackson.annotation.JsonIgnore" \
  target/keycloak-core/org/keycloak/representations/idm/authorization/ScopeRepresentation.java
