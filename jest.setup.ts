import "@testing-library/jest-dom";
import i18n from "i18next";
import { initReactI18next } from "react-i18next";

import "mutationobserver-shim";

i18n.use(initReactI18next).init({
  lng: "en",
  fallbackLng: "en",

  // have a common namespace used around the full app
  ns: ["translations"],
  defaultNS: "translations",

  resources: { en: { translations: {} } },
});

// eslint-disable-next-line no-undef
// @ts-ignore
global.MutationObserver = window.MutationObserver;
