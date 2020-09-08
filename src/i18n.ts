import i18n from "i18next";
import { initReactI18next } from "react-i18next";
// import backend from "i18next-http-backend";

import messages from "./messages.json";
import help from "./help.json";

const initOptions = {
  ns: ["messages", "help"],
  defaultNS: "messages",
  resources: { ...messages, ...help },
  lng: "en",
  fallbackLng: "en",
  saveMissing: true,

  interpolation: {
    escapeValue: false,
  },
};

i18n
  .use(initReactI18next)
  // .use(backend)
  .init(initOptions);

export { i18n, initOptions };
