import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type DashboardTab = "info" | "providers";

export type DashboardParams = { realm?: string; tab?: DashboardTab };

export const DashboardRoute: RouteDef = {
  path: "/:realm?/:tab?",
  component: lazy(() => import("../Dashboard")),
  breadcrumb: (t) => t("common:home"),
  access: "anyone",
  legacy: true,
};

export const toDashboard = (
  params: DashboardParams
): LocationDescriptorObject => ({
  pathname: generatePath(DashboardRoute.path, params),
});
