import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ClientTab =
  | "settings"
  | "keys"
  | "credentials"
  | "roles"
  | "clientScopes"
  | "advanced"
  | "mappers"
  | "authorization"
  | "serviceAccount"
  | "permissions"
  | "sessions";

export type ClientParams = {
  realm: string;
  clientId: string;
  tab: ClientTab;
};

export const ClientRoute: RouteDef = {
  path: "/:realm/clients/:clientId/:tab",
  component: lazy(() => import("../ClientDetails")),
  breadcrumb: (t) => t("clients:clientSettings"),
  access: "query-clients",
};

export const toClient = (params: ClientParams): Partial<Path> => ({
  pathname: generatePath(ClientRoute.path, params),
});
