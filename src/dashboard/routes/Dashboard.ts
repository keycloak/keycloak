import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type DashboardParams = { realm?: string };

export const DashboardRoute: RouteDef = {
  path: "/:realm?",
  component: lazy(() => import("../Dashboard")),
  breadcrumb: (t) => t("common:home"),
  access: "anyone",
};

export const toDashboard = (
  params: DashboardParams
): LocationDescriptorObject => ({
  pathname: generatePath(DashboardRoute.path, params),
});
