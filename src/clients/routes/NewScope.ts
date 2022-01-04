import type { LocationDescriptorObject } from "history";
import type { RouteDef } from "../../route-config";
import { generatePath } from "react-router-dom";
import { lazy } from "react";

export type NewScopeParams = { realm: string; id: string };

export const NewScopeRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/scope/new",
  component: lazy(() => import("../authorization/ScopeDetails")),
  breadcrumb: (t) => t("clients:createAuthorizationScope"),
  access: "manage-clients",
};

export const toNewScope = (
  params: NewScopeParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewScopeRoute.path, params),
});
