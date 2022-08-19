import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type ClientsTab = "list" | "initial-access-token";

export type ClientsParams = {
  realm: string;
  tab?: ClientsTab;
};

export const ClientsRoute: RouteDef = {
  path: "/:realm/clients/:tab?",
  component: lazy(() => import("../ClientsSection")),
  breadcrumb: (t) => t("clients:clientList"),
  access: "query-clients",
  legacy: true,
};

export const toClients = (params: ClientsParams): Partial<Path> => ({
  pathname: generatePath(ClientsRoute.path, params),
});
