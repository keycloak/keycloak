import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type WorkflowDetailParams = {
  realm: string;
  id: string;
  mode: "view" | "copy" | "create";
};

const WorkflowDetailForm = lazy(() => import("../WorkflowDetailForm"));

export const WorkflowDetailRoute: AppRouteObject = {
  path: "/:realm/workflows/:mode/:id",
  element: <WorkflowDetailForm />,
  breadcrumb: (t) => t("workflowDetails"),
  handle: {
    access: "anyone", // TODO: update access when view permission is added
  },
};

export const toWorkflowDetail = (
  params: WorkflowDetailParams,
): Partial<Path> => {
  return {
    pathname: generateEncodedPath(WorkflowDetailRoute.path, params),
  };
};
