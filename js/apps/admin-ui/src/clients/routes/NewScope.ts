import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type NewScopeParams = { realm: string; id: string };

export const NewScopeRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/scope/new",
  component: lazy(() => import("../authorization/ScopeDetails")),
  breadcrumb: (t) => t("clients:createAuthorizationScope"),
  access: "view-clients",
};

export const toNewScope = (params: NewScopeParams): Partial<Path> => ({
  pathname: generatePath(NewScopeRoute.path, params),
});
