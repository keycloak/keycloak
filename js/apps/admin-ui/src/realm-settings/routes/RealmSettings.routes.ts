import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

export type RealmSettingsTab =
  | "general"
  | "login"
  | "email"
  | "themes"
  | "keys"
  | "events"
  | "localization"
  | "security-defenses"
  | "sessions"
  | "tokens"
  | "client-policies"
  | "user-profile"
  | "user-registration";

export type RealmSettingsParams = {
  realm: string;
  tab?: RealmSettingsTab;
};

export const RealmSettingsRoutePath = "/:realm/realm-settings";
export const RealmSettingsRouteWithTabPath = "/:realm/realm-settings/:tab";

export const toRealmSettings = (params: RealmSettingsParams): Partial<Path> => {
  const path = params.tab
    ? RealmSettingsRouteWithTabPath
    : RealmSettingsRoutePath;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
