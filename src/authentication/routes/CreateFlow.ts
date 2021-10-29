import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type CreateFlowParams = { realm: string };

export const CreateFlowRoute: RouteDef = {
  path: "/:realm/authentication/flows/create",
  component: lazy(() => import("../form/CreateFlow")),
  breadcrumb: (t) => t("authentication:createFlow"),
  access: "manage-authorization",
};

export const toCreateFlow = (
  params: CreateFlowParams
): LocationDescriptorObject => ({
  pathname: generatePath(CreateFlowRoute.path, params),
});
