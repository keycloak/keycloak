import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ClientScopesSection } from "../ClientScopesSection";

export type ClientScopesParams = { realm: string };

export const ClientScopesRoute: RouteDef = {
  path: "/:realm/client-scopes",
  component: ClientScopesSection,
  breadcrumb: (t) => t("client-scopes:clientScopeList"),
  access: "view-clients",
};

export const toClientScopes = (
  params: ClientScopesParams
): LocationDescriptorObject => ({
  pathname: generatePath(ClientScopesRoute.path, params),
});
