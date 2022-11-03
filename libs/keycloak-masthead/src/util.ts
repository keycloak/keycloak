import Keycloak from "keycloak-js";

import { TranslateFunction } from "./translation/useTranslation";

export function loggedInUserName(keycloak: Keycloak, t: TranslateFunction) {
  if (!keycloak.tokenParsed) {
    return t("unknownUser");
  }

  const givenName = keycloak.tokenParsed.given_name;
  const familyName = keycloak.tokenParsed.family_name;
  const preferredUsername = keycloak.tokenParsed.preferred_username;

  if (givenName && familyName) {
    return t("fullName", { givenName, familyName });
  }

  return givenName || familyName || preferredUsername || t("unknownUser");
}
