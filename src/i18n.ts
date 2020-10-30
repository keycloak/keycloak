import i18n from "i18next";
import { initReactI18next } from "react-i18next";
// import backend from "i18next-http-backend";

import common from "./common-messages.json";
import help from "./common-help.json";
import clients from "./clients/messages.json";
import clientsHelp from "./clients/help.json";
import clientScopes from "./client-scopes/messages.json";
import clientScopesHelp from "./client-scopes/help.json";
import groups from "./groups/messages.json";
import realm from "./realm/messages.json";
import roles from "./realm-roles/messages.json";
import users from "./user/messages.json";
import sessions from "./sessions/messages.json";
import events from "./events/messages.json";
import storybook from "./stories/messages.json";
import userFederation from "./user-federation/messages.json";
import userFederationHelp from "./user-federation/help.json";

const initOptions = {
  defaultNS: "common",
  resources: {
    en: {
      ...common,
      ...help,
      ...clients,
      ...clientsHelp,
      ...clientScopes,
      ...clientScopesHelp,
      ...groups,
      ...realm,
      ...roles,
      ...groups,
      ...users,
      ...sessions,
      ...events,
      ...storybook,
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
