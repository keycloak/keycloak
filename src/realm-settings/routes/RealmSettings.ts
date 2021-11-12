import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type RealmSettingsTab =
  | "general"
  | "login"
  | "email"
  | "themes"
  | "keys"
  | "events"
  | "securityDefences"
  | "sessions"
  | "tokens"
  | "clientPolicies"
  | "userProfile";

export type RealmSettingsParams = {
  realm: string;
  tab?: RealmSettingsTab;
};

export const RealmSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/:tab?",
  component: lazy(() => import("../RealmSettingsSection")),
  breadcrumb: (t) => t("realmSettings"),
  access: "view-realm",
};

export const toRealmSettings = (
  params: RealmSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(RealmSettingsRoute.path, params),
});
