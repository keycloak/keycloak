import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { NewClientForm } from "../add/NewClientForm";

export type AddClientParams = { realm: string };

export const AddClientRoute: RouteDef = {
  path: "/:realm/clients/add-client",
  component: NewClientForm,
  breadcrumb: (t) => t("clients:createClient"),
  access: "manage-clients",
};

export const toAddClient = (
  params: AddClientParams
): LocationDescriptorObject => ({
  pathname: generatePath(AddClientRoute.path, params),
});
