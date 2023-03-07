import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ClientScopesParams = { realm: string };

export const ClientScopesRoute: RouteDef = {
  path: "/:realm/client-scopes",
  component: lazy(() => import("../ClientScopesSection")),
  breadcrumb: (t) => t("client-scopes:clientScopeList"),
  access: "view-clients",
};

export const toClientScopes = (params: ClientScopesParams): Partial<Path> => ({
  pathname: generatePath(ClientScopesRoute.path, params),
});
