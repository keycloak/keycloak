import { type KeycloakTokenParsed } from "keycloak-js";

import { TranslateFunction } from "./translation/useTranslation";

export function loggedInUserName(
  token: KeycloakTokenParsed | undefined,
  t: TranslateFunction,
) {
  if (!token) {
    return t("unknownUser");
  }

  const givenName = token.given_name;
  const familyName = token.family_name;
  const preferredUsername = token.preferred_username;

  if (givenName && familyName) {
    return t("fullName", { givenName, familyName });
  }

  return givenName || familyName || preferredUsername || t("unknownUser");
}
