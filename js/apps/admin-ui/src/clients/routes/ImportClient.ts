import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ImportClientParams = { realm: string };

export const ImportClientRoute: RouteDef = {
  path: "/:realm/clients/import-client",
  component: lazy(() => import("../import/ImportForm")),
  breadcrumb: (t) => t("clients:importClient"),
  access: "manage-clients",
};

export const toImportClient = (params: ImportClientParams): Partial<Path> => ({
  pathname: generatePath(ImportClientRoute.path, params),
});
