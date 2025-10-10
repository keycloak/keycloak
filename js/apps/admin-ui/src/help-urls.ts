const adminGuide =
  "https://www.keycloak.org/docs/latest/server_admin/index.html";

const keycloakHomepageURL = "https://www.keycloak.org";

export default {
  documentationUrl: adminGuide,
  clientsUrl: `${adminGuide}#assembly-managing-clients_server_administration_guide`,
  clientScopesUrl: `${adminGuide}#_client_scopes`,
  realmRolesUrl: `${adminGuide}#assigning-permissions-using-roles-and-groups`,
  usersUrl: `${adminGuide}#assembly-managing-users_server_administration_guide`,
  groupsUrl: `${adminGuide}#proc-managing-groups_server_administration_guide`,
  sessionsUrl: `${adminGuide}#managing-user-sessions`,
  eventsUrl: `${adminGuide}#configuring-auditing-to-track-events`,
  realmSettingsUrl: `${adminGuide}#configuring-realms`,
  authenticationUrl: `${adminGuide}#configuring-authentication`,
  identityProvidersUrl: `${adminGuide}#_identity_broker`,
  userFederationUrl: `${adminGuide}#_user-storage-federation`,
  documentation: `${keycloakHomepageURL}/documentation`,
  guides: `${keycloakHomepageURL}/guides`,
  community: `${keycloakHomepageURL}/community`,
  blog: `${keycloakHomepageURL}/blog`,
  workflowsUrl: `https://www.keycloak.org/2025/10/workflows-experimental-26-4`,
};
