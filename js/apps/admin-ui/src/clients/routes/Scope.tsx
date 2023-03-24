import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ScopeDetailsParams = {
  realm: string;
  id: string;
  scopeId?: string;
};

const ScopeDetails = lazy(() => import("../authorization/ScopeDetails"));

export const ScopeDetailsRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/scope",
  element: <ScopeDetails />,
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
