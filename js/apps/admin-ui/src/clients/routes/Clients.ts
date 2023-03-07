import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ClientsTab =
  | "list"
  | "initial-access-token"
  | "client-registration";

export type ClientsParams = {
  realm: string;
  tab?: ClientsTab;
};

export const ClientsRoute: RouteDef = {
  path: "/:realm/clients",
  component: lazy(() => import("../ClientsSection")),
  breadcrumb: (t) => t("clients:clientList"),
  access: "query-clients",
};

export const ClientsRouteWithTab: RouteDef = {
  ...ClientsRoute,
  path: "/:realm/clients/:tab",
};

export const toClients = (params: ClientsParams): Partial<Path> => {
  const path = params.tab ? ClientsRouteWithTab.path : ClientsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
