import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type ThemesTabType = "settings" | "lightColors" | "darkColors";

export type ThemesParams = {
  realm: string;
  tab: ThemesTabType;
};

const RealmSettingsSection = lazy(() => import("../RealmSettingsSection"));

export const ThemeTabRoute: AppRouteObject = {
  path: "/:realm/realm-settings/themes/:tab",
  element: <RealmSettingsSection />,
  breadcrumb: (t) => t("themes"),
  handle: {
    access: "view-realm",
  },
};

export const toThemesTab = (params: ThemesParams): Partial<Path> => ({
  pathname: generateEncodedPath(ThemeTabRoute.path, params),
});
