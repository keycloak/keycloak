import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type KeySubTab = "list" | "providers";

export type KeysParams = {
  realm: string;
  tab: KeySubTab;
};

export const KeysRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:tab",
  component: lazy(() => import("../RealmSettingsSection")),
  breadcrumb: (t) => t("realm-settings:keys"),
  access: "view-realm",
};

export const toKeysTab = (params: KeysParams): Partial<Path> => ({
  pathname: generatePath(KeysRoute.path, params),
});
