import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type AddClientParams = { realm: string };

const NewClientForm = lazy(() => import("../add/NewClientForm"));

export const AddClientRoute: AppRouteObject = {
  path: "/:realm/clients/add-client",
  element: <NewClientForm />,
  handle: {
    access: "manage-clients",
    breadcrumb: (t) => t("createClient"),
  },
};

export const toAddClient = (params: AddClientParams): Partial<Path> => ({
  pathname: generateEncodedPath(AddClientRoute.path, params),
});
