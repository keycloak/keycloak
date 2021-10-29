import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ClientsTab = "list" | "initialAccessToken";

export type ClientsParams = {
  realm: string;
  tab?: ClientsTab;
};

export const ClientsRoute: RouteDef = {
  path: "/:realm/clients/:tab?",
  component: lazy(() => import("../ClientsSection")),
  breadcrumb: (t) => t("clients:clientList"),
  access: "query-clients",
};

export const toClients = (params: ClientsParams): LocationDescriptorObject => ({
  pathname: generatePath(ClientsRoute.path, params),
});
