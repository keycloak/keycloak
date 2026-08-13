import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type CreateFlowParams = { realm: string };

const CreateFlow = lazy(() => import("../form/CreateFlow"));

export const CreateFlowRoute: AppRouteObject = {
  path: "/:realm/authentication/flows/create",
  element: <CreateFlow />,
  handle: {
    access: "manage-authorization",
    breadcrumb: (t) => t("createFlow"),
  },
};

export const toCreateFlow = (params: CreateFlowParams): Partial<Path> => ({
  pathname: generateEncodedPath(CreateFlowRoute.path, params),
});
