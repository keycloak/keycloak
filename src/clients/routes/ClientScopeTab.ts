import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ClientScopesTab = "setup" | "evaluate";

export type ClientScopesParams = {
  realm: string;
  clientId: string;
  tab: ClientScopesTab;
};
export const ClientScopesRoute: RouteDef = {
  path: "/:realm/clients/:clientId/clientScopes/:tab",
  component: lazy(() => import("../ClientDetails")),
  breadcrumb: (t) => t("clients:clientSettings"),
  access: "view-clients",
};

export const toClientScopesTab = (
  params: ClientScopesParams
): LocationDescriptorObject => ({
  pathname: generatePath(ClientScopesRoute.path, params),
});
