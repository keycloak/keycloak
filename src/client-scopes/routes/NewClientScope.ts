import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ClientScopeForm } from "../form/ClientScopeForm";

export type NewClientScopeParams = { realm: string };

export const NewClientScopeRoute: RouteDef = {
  path: "/:realm/client-scopes/new",
  component: ClientScopeForm,
  breadcrumb: (t) => t("client-scopes:createClientScope"),
  access: "manage-clients",
};

export const toNewClientScope = (
  params: NewClientScopeParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewClientScopeRoute.path, params),
});
