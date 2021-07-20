import i18n from "i18next";
import { initReactI18next } from "react-i18next";
// import backend from "i18next-http-backend";

import common from "./common-messages";
import help from "./common-help";
import dashboard from "./dashboard/messages";
import clients from "./clients/messages";
import clientsHelp from "./clients/help";
import clientScopes from "./client-scopes/messages";
import clientScopesHelp from "./client-scopes/help";
import groups from "./groups/messages";
import realm from "./realm/messages";
import roles from "./realm-roles/messages";
import users from "./user/messages";
import usersHelp from "./user/help";
import sessions from "./sessions/messages";
import events from "./events/messages";
import realmSettings from "./realm-settings/messages";
import realmSettingsHelp from "./realm-settings/help";
import authentication from "./authentication/messages";
import userFederation from "./user-federation/messages";
import userFederationHelp from "./user-federation/help";
import identityProviders from "./identity-providers/messages";
import identityProvidersHelp from "./identity-providers/help";

const initOptions = {
  defaultNS: "common",
  resources: {
    en: {
      ...common,
      ...help,
      ...dashboard,
      ...clients,
      ...clientsHelp,
      ...clientScopes,
      ...clientScopesHelp,
      ...groups,
      ...realm,
      ...roles,
      ...groups,
      ...users,
      ...usersHelp,
      ...sessions,
      ...userFederation,
      ...events,
      ...realmSettings,
      ...realmSettingsHelp,
      ...authentication,
      ...identityProviders,
      ...identityProvidersHelp,
      ...userFederation,
      ...userFederationHelp,
    },
  },
  lng: "en",
  fallbackLng: "en",

  interpolation: {
    escapeValue: false,
  },
};

i18n
  .use(initReactI18next)
  // .use(backend)
  .init(initOptions);

export default i18n;
