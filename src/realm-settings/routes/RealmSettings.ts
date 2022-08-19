import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

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

export const RealmSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/:tab?",
  component: lazy(() => import("../RealmSettingsSection")),
  breadcrumb: (t) => t("realmSettings"),
  access: "view-realm",
  legacy: true,
};

export const toRealmSettings = (
  params: RealmSettingsParams
): Partial<Path> => ({
  pathname: generatePath(RealmSettingsRoute.path, params),
});
