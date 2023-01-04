import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type ClientScopeTab = "settings" | "mappers" | "scope";

export type ClientScopeParams = {
  realm: string;
  id: string;
  tab: ClientScopeTab;
};

export const ClientScopeRoute: RouteDef = {
  path: "/:realm/client-scopes/:id/:tab",
  component: lazy(() => import("../EditClientScope")),
  breadcrumb: (t) => t("client-scopes:clientScopeDetails"),
  access: "view-clients",
};

export const toClientScope = (params: ClientScopeParams): Partial<Path> => {
  const path = ClientScopeRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
