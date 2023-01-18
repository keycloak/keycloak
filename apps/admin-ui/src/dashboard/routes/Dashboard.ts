import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type DashboardTab = "info" | "providers";

export type DashboardParams = { realm?: string; tab?: DashboardTab };

export const DashboardRoute: RouteDef = {
  path: "/",
  component: lazy(() => import("../Dashboard")),
  breadcrumb: (t) => t("common:home"),
  access: "anyone",
};

export const DashboardRouteWithRealm: RouteDef = {
  ...DashboardRoute,
  path: "/:realm",
};

export const DashboardRouteWithTab: RouteDef = {
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
    pathname: generatePath(pathname, params),
  };
};
