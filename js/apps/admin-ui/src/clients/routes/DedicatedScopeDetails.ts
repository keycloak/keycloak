import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type DedicatedScopeTab = "mappers" | "scope";

export type DedicatedScopeDetailsParams = {
  realm: string;
  clientId: string;
  tab?: DedicatedScopeTab;
};

export const DedicatedScopeDetailsRoute: RouteDef = {
  path: "/:realm/clients/:clientId/clientScopes/dedicated",
  component: lazy(() => import("../scopes/DedicatedScopes")),
  breadcrumb: (t) => t("clients:dedicatedScopes"),
  access: "view-clients",
};

export const DedicatedScopeDetailsWithTabRoute: RouteDef = {
  ...DedicatedScopeDetailsRoute,
  path: "/:realm/clients/:clientId/clientScopes/dedicated/:tab",
};

export const toDedicatedScope = (
  params: DedicatedScopeDetailsParams
): Partial<Path> => {
  const path = params.tab
    ? DedicatedScopeDetailsWithTabRoute.path
    : DedicatedScopeDetailsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
