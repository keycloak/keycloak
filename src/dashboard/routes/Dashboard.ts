import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { DashboardSection } from "../Dashboard";

export type DashboardParams = { realm?: string };

export const DashboardRoute: RouteDef = {
  path: "/:realm?",
  component: DashboardSection,
  breadcrumb: (t) => t("common:home"),
  access: "anyone",
};

export const toDashboard = (
  params: DashboardParams
): LocationDescriptorObject => ({
  pathname: generatePath(DashboardRoute.path, params),
});
