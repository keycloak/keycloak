import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type ClientsTab =
  | "list"
  | "initial-access-token"
  | "client-registration";

export type ClientsParams = {
  realm: string;
  tab?: ClientsTab;
};

const ClientsSection = lazy(() => import("../ClientsSection"));

export const ClientsRoute: AppRouteObject = {
  path: "/:realm/clients",
  element: <ClientsSection />,
  breadcrumb: (t) => t("clients:clientList"),
  handle: {
    access: "query-clients",
  },
};

export const ClientsRouteWithTab: AppRouteObject = {
  ...ClientsRoute,
  path: "/:realm/clients/:tab",
};

export const toClients = (params: ClientsParams): Partial<Path> => {
  const path = params.tab ? ClientsRouteWithTab.path : ClientsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
