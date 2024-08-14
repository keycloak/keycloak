import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type DashboardTab = "info" | "providers" | "welcome";

export type DashboardParams = { realm?: string; tab?: DashboardTab };

const Dashboard = lazy(() => import("../Dashboard"));

export const DashboardRoute: AppRouteObject = {
  path: "/",
  element: <Dashboard />,
  breadcrumb: (t) => t("home"),
  handle: {
    access: "anyone",
  },
};

export const DashboardRouteWithRealm: AppRouteObject = {
  ...DashboardRoute,
  path: "/:realm",
};

export const DashboardRouteWithTab: AppRouteObject = {
  ...DashboardRoute,
  path: "/:realm/:tab",
};

export const toDashboard = (params: DashboardParams): Partial<Path> => {
  const pathname = params.realm
    ? params.tab
      ? DashboardRouteWithTab.path
      : DashboardRouteWithRealm.path
    : DashboardRoute.path;

  return {
    pathname: generateEncodedPath(pathname, params),
  };
};
