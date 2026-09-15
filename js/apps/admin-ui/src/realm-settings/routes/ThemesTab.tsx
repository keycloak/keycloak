import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type ThemesTabType = "settings" | "quickTheme";

export type ThemesParams = {
  realm: string;
  tab: ThemesTabType;
};

const RealmSettingsSection = lazy(() => import("../RealmSettingsSection"));

export const ThemeTabRoute: AppRouteObject = {
  path: "/:realm/realm-settings/themes/:tab",
  element: <RealmSettingsSection />,
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("themes"),
  },
};

export const toThemesTab = (params: ThemesParams): Partial<Path> => ({
  pathname: generateEncodedPath(ThemeTabRoute.path, params),
});
