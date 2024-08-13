import { lazy } from "react";
import { generatePath, type Path } from "react-router-dom";
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
  pathname: generatePath(AddRealmRoute.path, params),
});
