import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ClientScopeForm } from "../form/ClientScopeForm";

export type ClientScopeParams = {
  realm: string;
  id: string;
  type: string;
  tab: string;
};

export const ClientScopeRoute: RouteDef = {
  path: "/:realm/client-scopes/:id/:type/:tab",
  component: ClientScopeForm,
  breadcrumb: (t) => t("client-scopes:clientScopeDetails"),
  access: "view-clients",
};

export const toClientScope = (
  params: ClientScopeParams
): LocationDescriptorObject => ({
  pathname: generatePath(ClientScopeRoute.path, params),
});
