import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { EditExecutorCrumb } from "../RealmSettingsSection";

export type ExecutorParams = {
  realm: string;
  profileName: string;
  executorName: string;
};

export const ExecutorRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/:profileName/:executorName",
  component: lazy(() => import("../ExecutorForm")),
  breadcrumb: () => EditExecutorCrumb,
  access: ["manage-realm"],
};

export const toExecutor = (
  params: ExecutorParams
): LocationDescriptorObject => ({
  pathname: generatePath(ExecutorRoute.path, params),
});
