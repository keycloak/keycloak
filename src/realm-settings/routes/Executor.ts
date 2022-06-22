import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ExecutorParams = {
  realm: string;
  profileName: string;
  executorName: string;
};

export const ExecutorRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies/:profileName/edit-profile/:executorName",
  component: lazy(() => import("../ExecutorForm")),
  breadcrumb: (t) => t("realm-settings:executorDetails"),
  access: ["manage-realm"],
};

export const toExecutor = (
  params: ExecutorParams
): LocationDescriptorObject => ({
  pathname: generatePath(ExecutorRoute.path, params),
});
