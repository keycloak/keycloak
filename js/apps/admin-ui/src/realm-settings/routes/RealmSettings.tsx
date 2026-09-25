import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  RealmSettingsRoutePath,
  RealmSettingsRouteWithTabPath,
  type RealmSettingsParams,
  type RealmSettingsTab,
  toRealmSettings,
} from "./RealmSettings.routes";

export type { RealmSettingsParams, RealmSettingsTab };
export { toRealmSettings };

const RealmSettingsSection = lazy(() => import("../RealmSettingsSection"));

export const RealmSettingsRoute: AppRouteObject = {
  path: RealmSettingsRoutePath,
  element: <RealmSettingsSection />,
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("realmSettings"),
  },
};

export const RealmSettingsRouteWithTab: AppRouteObject = {
  ...RealmSettingsRoute,
  path: RealmSettingsRouteWithTabPath,
};
