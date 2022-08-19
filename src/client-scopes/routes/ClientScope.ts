import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type ClientScopeTab = "settings" | "mappers" | "scope";

export type ClientScopeParams = {
  realm: string;
  id: string;
  tab: ClientScopeTab;
  type?: string;
};

export const ClientScopeRoute: RouteDef = {
  path: "/:realm/client-scopes/:id/:tab/:type?",
  component: lazy(() => import("../form/ClientScopeForm")),
  breadcrumb: (t) => t("client-scopes:clientScopeDetails"),
  access: "view-clients",
  legacy: true,
};

export const toClientScope = (params: ClientScopeParams): Partial<Path> => ({
  pathname: generatePath(ClientScopeRoute.path, params),
});
