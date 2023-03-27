import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type CreateFlowParams = { realm: string };

const CreateFlow = lazy(() => import("../form/CreateFlow"));

export const CreateFlowRoute: RouteDef = {
  path: "/:realm/authentication/flows/create",
  element: <CreateFlow />,
  breadcrumb: (t) => t("authentication:createFlow"),
  handle: {
    access: "manage-authorization",
  },
};

export const toCreateFlow = (params: CreateFlowParams): Partial<Path> => ({
  pathname: generatePath(CreateFlowRoute.path, params),
});
