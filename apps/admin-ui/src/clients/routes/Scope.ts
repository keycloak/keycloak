import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ScopeDetailsParams = {
  realm: string;
  id: string;
  scopeId?: string;
};

export const ScopeDetailsRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/scope",
  component: lazy(() => import("../authorization/ScopeDetails")),
  breadcrumb: (t) => t("clients:createAuthorizationScope"),
  access: "manage-clients",
};

export const ScopeDetailsWithScopeIdRoute: RouteDef = {
  ...ScopeDetailsRoute,
  path: "/:realm/clients/:id/authorization/scope/:scopeId",
};

export const toScopeDetails = (params: ScopeDetailsParams): Partial<Path> => {
  const path = params.scopeId
    ? ScopeDetailsWithScopeIdRoute.path
    : ScopeDetailsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
