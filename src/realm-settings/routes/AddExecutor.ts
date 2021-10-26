import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ExecutorForm } from "../ExecutorForm";

export type AddExecutorParams = {
  realm: string;
  profileName: string;
};

export const AddExecutorRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/:profileName/add-executor",
  component: ExecutorForm,
  breadcrumb: (t) => t("realm-settings:addExecutor"),
  access: "manage-realm",
};

export const toAddExecutor = (
  params: AddExecutorParams
): LocationDescriptorObject => ({
  pathname: generatePath(AddExecutorRoute.path, params),
});
