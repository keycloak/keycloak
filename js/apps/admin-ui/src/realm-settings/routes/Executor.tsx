import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type ExecutorParams = {
  realm: string;
  profileName: string;
  executorName: string;
};

const ExecutorForm = lazy(() => import("../ExecutorForm"));

export const ExecutorRoute: AppRouteObject = {
  path: "/:realm/realm-settings/client-policies/:profileName/edit-profile/:executorName",
  element: <ExecutorForm />,
  breadcrumb: (t) => t("executorDetails"),
  handle: {
    access: ["manage-realm"],
  },
};

export const toExecutor = (params: ExecutorParams): Partial<Path> => ({
  pathname: generateEncodedPath(ExecutorRoute.path, params),
});
