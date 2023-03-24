import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AddRealmParams = { realm: string };

const NewRealmForm = lazy(() => import("../add/NewRealmForm"));

export const AddRealmRoute: RouteDef = {
  path: "/:realm/add-realm",
  element: <NewRealmForm />,
  breadcrumb: (t) => t("realm:createRealm"),
  access: "view-realm",
};

export const toAddRealm = (params: AddRealmParams): Partial<Path> => ({
  pathname: generatePath(AddRealmRoute.path, params),
});
