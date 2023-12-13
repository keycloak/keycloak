import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type AddRealmParams = { realm: string };

const NewRealmForm = lazy(() => import("../add/NewRealmForm"));

export const AddRealmRoute: AppRouteObject = {
  path: "/:realm/add-realm",
  element: <NewRealmForm />,
  breadcrumb: (t) => t("createRealm"),
  handle: {
    access: "view-realm",
  },
};

export const toAddRealm = (params: AddRealmParams): Partial<Path> => ({
  pathname: generateEncodedPath(AddRealmRoute.path, params),
});
