import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { RealmSettingsSection } from "../RealmSettingsSection";

export type RealmSettingsTab =
  | "general"
  | "login"
  | "email"
  | "themes"
  | "keys"
  | "events"
  | "securityDefences"
  | "sessions"
  | "tokens";

export type RealmSettingsParams = {
  realm: string;
  tab?: RealmSettingsTab;
};

export const RealmSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/:tab?",
  component: RealmSettingsSection,
  breadcrumb: (t) => t("realmSettings"),
  access: "view-realm",
};

export const toRealmSettings = (
  params: RealmSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(RealmSettingsRoute.path, params),
});
