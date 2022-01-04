import type { LocationDescriptorObject } from "history";
import type { RouteDef } from "../../route-config";
import { generatePath } from "react-router-dom";
import { lazy } from "react";

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
};

export const toScopeDetails = (
  params: ScopeDetailsParams
): LocationDescriptorObject => ({
  pathname: generatePath(ScopeDetailsRoute.path, params),
});
