import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type KeySubTab = "list" | "providers";

export type KeysParams = {
  realm: string;
  tab: KeySubTab;
};

const RealmSettingsSection = lazy(() => import("../RealmSettingsSection"));

export const KeysRoute: AppRouteObject = {
  path: "/:realm/realm-settings/keys/:tab",
  element: <RealmSettingsSection />,
  breadcrumb: (t) => t("realm-settings:keys"),
  handle: {
    access: "view-realm",
  },
};

export const toKeysTab = (params: KeysParams): Partial<Path> => ({
  pathname: generatePath(KeysRoute.path, params),
});
