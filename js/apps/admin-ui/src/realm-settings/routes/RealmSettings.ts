import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
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
  path: "/:realm/realm-settings",
  component: lazy(() => import("../RealmSettingsSection")),
  breadcrumb: (t) => t("realmSettings"),
  access: "view-realm",
};

export const RealmSettingsRouteWithTab: RouteDef = {
  ...RealmSettingsRoute,
  path: "/:realm/realm-settings/:tab",
};

export const toRealmSettings = (params: RealmSettingsParams): Partial<Path> => {
  const path = params.tab
    ? RealmSettingsRouteWithTab.path
    : RealmSettingsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
