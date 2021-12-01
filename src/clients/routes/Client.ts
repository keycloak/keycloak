import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ClientTab =
  | "settings"
  | "roles"
  | "clientScopes"
  | "advanced"
  | "mappers"
  | "authorization";

export type ClientParams = {
  realm: string;
  clientId: string;
  tab: ClientTab;
};

export const ClientRoute: RouteDef = {
  path: "/:realm/clients/:clientId/:tab",
  component: lazy(() => import("../ClientDetails")),
  breadcrumb: (t) => t("clients:clientSettings"),
  access: "view-clients",
};

export const toClient = (params: ClientParams): LocationDescriptorObject => ({
  pathname: generatePath(ClientRoute.path, params),
});
