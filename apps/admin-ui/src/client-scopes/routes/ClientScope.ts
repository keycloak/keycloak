import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type ClientScopeTab = "settings" | "mappers" | "scope";

export type ClientScopeParams = {
  realm: string;
  id: string;
  tab: ClientScopeTab;
  type?: string;
};

export const ClientScopeRoute: RouteDef = {
  path: "/:realm/client-scopes/:id/:tab",
  component: lazy(() => import("../form/ClientScopeForm")),
  breadcrumb: (t) => t("client-scopes:clientScopeDetails"),
  access: "view-clients",
};

export const ClientScopeWithTypeRoute: RouteDef = {
  ...ClientScopeRoute,
  path: "/:realm/client-scopes/:id/:tab/:type",
};

export const toClientScope = (params: ClientScopeParams): Partial<Path> => {
  const path = params.type
    ? ClientScopeWithTypeRoute.path
    : ClientScopeRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
