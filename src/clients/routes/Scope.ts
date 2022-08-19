import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type ScopeDetailsParams = {
  realm: string;
  id: string;
  scopeId?: string;
};

export const ScopeDetailsRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/scope/:scopeId?",
  component: lazy(() => import("../authorization/ScopeDetails")),
  breadcrumb: (t) => t("clients:createAuthorizationScope"),
  access: "manage-clients",
  legacy: true,
};

export const toScopeDetails = (params: ScopeDetailsParams): Partial<Path> => ({
  pathname: generatePath(ScopeDetailsRoute.path, params),
});
