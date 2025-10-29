import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type WorkflowsParams = { realm: string };

const WorkflowsSection = lazy(() => import("../WorkflowsSection"));

export const WorkflowsRoute: AppRouteObject = {
  path: "/:realm/workflows",
  element: <WorkflowsSection />,
  breadcrumb: (t) => t("workflows"),
  handle: {
    access: "manage-realm",
  },
};

export const toWorkflows = (params: WorkflowsParams): Partial<Path> => ({
  pathname: generateEncodedPath(WorkflowsRoute.path, params),
});
