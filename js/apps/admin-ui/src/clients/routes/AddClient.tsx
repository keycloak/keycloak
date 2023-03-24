import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AddClientParams = { realm: string };

const NewClientForm = lazy(() => import("../add/NewClientForm"));

export const AddClientRoute: RouteDef = {
  path: "/:realm/clients/add-client",
  element: <NewClientForm />,
  breadcrumb: (t) => t("clients:createClient"),
  access: "manage-clients",
};

export const toAddClient = (params: AddClientParams): Partial<Path> => ({
  pathname: generatePath(AddClientRoute.path, params),
});
