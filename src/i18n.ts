import i18n from "i18next";
import { initReactI18next } from "react-i18next";
// import backend from "i18next-http-backend";

import common from "./common-messages.json";
import clients from "./clients/messages.json";
import clientScopes from "./client-scopes/messages.json";
import realm from "./realm/messages.json";
import roles from "./realm-roles/messages.json";
import help from "./help.json";

const initOptions = {
  defaultNS: "common",
  resources: {
    en: { ...common, ...help, ...clients, ...clientScopes, ...realm, ...roles },
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
