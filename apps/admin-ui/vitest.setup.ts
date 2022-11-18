import matchers from "@testing-library/jest-dom/matchers";
import { use } from "i18next";
import { initReactI18next } from "react-i18next";
import { expect } from "vitest";

expect.extend(matchers);

use(initReactI18next).init({
  lng: "en",
  fallbackLng: "en",

  // have a common namespace used around the full app
  ns: ["translations"],
  defaultNS: "translations",

  resources: { en: { translations: {} } },
});
