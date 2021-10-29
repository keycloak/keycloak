import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AddClientParams = { realm: string };

export const AddClientRoute: RouteDef = {
  path: "/:realm/clients/add-client",
  component: lazy(() => import("../add/NewClientForm")),
  breadcrumb: (t) => t("clients:createClient"),
  access: "manage-clients",
};

export const toAddClient = (
  params: AddClientParams
): LocationDescriptorObject => ({
  pathname: generatePath(AddClientRoute.path, params),
});
