import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type AddWorkflowParams = { realm: string };

const CreateWorkflow = lazy(() => import("../CreateWorkflow"));

export const AddWorkflowRoute: AppRouteObject = {
  path: "/:realm/workflows/new",
  element: <CreateWorkflow />,
  breadcrumb: (t) => t("createWorkflow"),
  handle: {
    access: "manage-realm",
  },
};

export const toAddWorkflow = (params: AddWorkflowParams): Partial<Path> => ({
  pathname: generateEncodedPath(AddWorkflowRoute.path, params),
});
