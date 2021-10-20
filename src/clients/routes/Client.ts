import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ClientDetails } from "../ClientDetails";

export type ClientTab =
  | "settings"
  | "roles"
  | "clientScopes"
  | "advanced"
  | "mappers";

export type ClientParams = {
  realm: string;
  clientId: string;
  tab: ClientTab;
};

export const ClientRoute: RouteDef = {
  path: "/:realm/clients/:clientId/:tab",
  component: ClientDetails,
  breadcrumb: (t) => t("clients:clientSettings"),
  access: "view-clients",
};

export const toClient = (params: ClientParams): LocationDescriptorObject => ({
  pathname: generatePath(ClientRoute.path, params),
});
