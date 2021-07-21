import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ClientsSection } from "../ClientsSection";

export type ClientsTab = "list" | "initialAccessToken";

export type ClientsParams = {
  realm: string;
  tab?: ClientsTab;
};

export const ClientsRoute: RouteDef = {
  path: "/:realm/clients/:tab?",
  component: ClientsSection,
  breadcrumb: (t) => t("clients:clientList"),
  access: "query-clients",
};

export const toClients = (params: ClientsParams): LocationDescriptorObject => ({
  pathname: generatePath(ClientsRoute.path, params),
});
