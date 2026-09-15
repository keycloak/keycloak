import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

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
  | "sessions"
  | "events"
  | "ssf";

export type ClientParams = {
  realm: string;
  clientId: string;
  tab: ClientTab;
};

const ClientDetails = lazy(() => import("../ClientDetails"));

export const ClientRoute: AppRouteObject = {
  path: "/:realm/clients/:clientId/:tab",
  element: <ClientDetails />,
  handle: {
    access: "query-clients",
    breadcrumb: (t) => t("clientSettings"),
  },
};

export const toClient = (params: ClientParams): Partial<Path> => ({
  pathname: generateEncodedPath(ClientRoute.path, params),
});
