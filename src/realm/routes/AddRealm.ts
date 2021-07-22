import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { NewRealmForm } from "../add/NewRealmForm";

export type AddRealmParams = { realm: string };

export const AddRealmRoute: RouteDef = {
  path: "/:realm/add-realm",
  component: NewRealmForm,
  breadcrumb: (t) => t("realm:createRealm"),
  access: "manage-realm",
};

export const toAddRealm = (
  params: AddRealmParams
): LocationDescriptorObject => ({
  pathname: generatePath(AddRealmRoute.path, params),
});
