import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type DedicatedScopeTab = "mappers" | "scope";

export type DedicatedScopeDetailsParams = {
  realm: string;
  clientId: string;
  tab?: DedicatedScopeTab;
};

export const DedicatedScopeDetailsRoute: RouteDef = {
  path: "/:realm/clients/:clientId/clientScopes/dedicated/:tab?",
  component: lazy(() => import("../scopes/DedicatedScopes")),
  breadcrumb: (t) => t("clients:dedicatedScopes"),
  access: "view-clients",
  legacy: true,
};

export const toDedicatedScope = (
  params: DedicatedScopeDetailsParams
): Partial<Path> => ({
  pathname: generatePath(DedicatedScopeDetailsRoute.path, params),
});
