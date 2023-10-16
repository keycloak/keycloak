import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
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
  breadcrumb: (t) => t("keys"),
  handle: {
    access: "view-realm",
  },
};

export const toKeysTab = (params: KeysParams): Partial<Path> => ({
  pathname: generateEncodedPath(KeysRoute.path, params),
});
