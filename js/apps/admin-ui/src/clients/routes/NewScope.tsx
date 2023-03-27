import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type NewScopeParams = { realm: string; id: string };

const ScopeDetails = lazy(() => import("../authorization/ScopeDetails"));

export const NewScopeRoute: AppRouteObject = {
  path: "/:realm/clients/:id/authorization/scope/new",
  element: <ScopeDetails />,
  breadcrumb: (t) => t("clients:createAuthorizationScope"),
  handle: {
    access: "view-clients",
  },
};

export const toNewScope = (params: NewScopeParams): Partial<Path> => ({
  pathname: generatePath(NewScopeRoute.path, params),
});
