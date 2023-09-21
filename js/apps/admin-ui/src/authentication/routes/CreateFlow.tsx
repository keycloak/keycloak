import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type CreateFlowParams = { realm: string };

const CreateFlow = lazy(() => import("../form/CreateFlow"));

export const CreateFlowRoute: AppRouteObject = {
  path: "/:realm/authentication/flows/create",
  element: <CreateFlow />,
  breadcrumb: (t) => t("createFlow"),
  handle: {
    access: "manage-authorization",
  },
};

export const toCreateFlow = (params: CreateFlowParams): Partial<Path> => ({
  pathname: generatePath(CreateFlowRoute.path, params),
});
