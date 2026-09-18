import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  ClientsRoutePath,
  ClientsRouteWithTabPath,
  type ClientsParams,
  type ClientsTab,
  toClients,
} from "./Clients.routes";

export type { ClientsParams, ClientsTab };
export { toClients };

const ClientsSection = lazy(() => import("../ClientsSection"));

export const ClientsRoute: AppRouteObject = {
  path: ClientsRoutePath,
  element: <ClientsSection />,
  handle: {
    access: "query-clients",
    breadcrumb: (t) => t("clientList"),
  },
};

export const ClientsRouteWithTab: AppRouteObject = {
  ...ClientsRoute,
  path: ClientsRouteWithTabPath,
};
