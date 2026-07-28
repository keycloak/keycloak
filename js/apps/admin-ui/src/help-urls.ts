// Every "Learn more" link, the masthead help menu, and the dashboard buttons
// resolve from here. The upstream values pointed at keycloak.org section
// anchors; those anchors mean nothing off that site, so each entry collapses to
// the Fidar site until there is a documentation host to deep-link into. When
// one exists, set `docsBase` and restore the per-section fragments.
const docsBase = "https://www.fidar.ai";

const adminGuide = docsBase;

export default {
  documentationUrl: adminGuide,
  clientsUrl: adminGuide,
  clientScopesUrl: adminGuide,
  realmRolesUrl: adminGuide,
  usersUrl: adminGuide,
  groupsUrl: adminGuide,
  orgGroupsUrl: adminGuide,
  sessionsUrl: adminGuide,
  eventsUrl: adminGuide,
  realmSettingsUrl: adminGuide,
  authenticationUrl: adminGuide,
  identityProvidersUrl: adminGuide,
  userFederationUrl: adminGuide,
  documentation: docsBase,
  guides: docsBase,
  community: docsBase,
  blog: docsBase,
  workflowsUrl: adminGuide,
};
