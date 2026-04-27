import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type WorkflowDetailParams = {
  realm: string;
  id: string;
  mode: "update" | "copy" | "create";
};

const WorkflowDetailForm = lazy(() => import("../WorkflowDetailForm"));

export const WorkflowDetailRoute: AppRouteObject = {
  path: "/:realm/workflows/:mode/:id",
  element: <WorkflowDetailForm />,
  handle: {
    access: "manage-realm",
    breadcrumb: (t) => t("workflowDetails"),
  },
};

export const toWorkflowDetail = (
  params: WorkflowDetailParams,
): Partial<Path> => {
  return {
    pathname: generateEncodedPath(WorkflowDetailRoute.path, params),
  };
};
