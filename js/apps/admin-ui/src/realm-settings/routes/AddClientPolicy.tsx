import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type AddClientPolicyParams = { realm: string };

const NewClientPolicyForm = lazy(() => import("../NewClientPolicyForm"));

export const AddClientPolicyRoute: AppRouteObject = {
  path: "/:realm/realm-settings/client-policies/policies/add-client-policy",
  element: <NewClientPolicyForm />,
  breadcrumb: (t) => t("realm-settings:createPolicy"),
  handle: {
    access: "manage-clients",
  },
};

export const toAddClientPolicy = (
  params: AddClientPolicyParams
): Partial<Path> => ({
  pathname: generatePath(AddClientPolicyRoute.path, params),
});
