import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type ImportClientParams = { realm: string };

const ImportForm = lazy(() => import("../import/ImportForm"));

export const ImportClientRoute: AppRouteObject = {
  path: "/:realm/clients/import-client",
  element: <ImportForm />,
  breadcrumb: (t) => t("importClient"),
  handle: {
    access: "manage-clients",
  },
};

export const toImportClient = (params: ImportClientParams): Partial<Path> => ({
  pathname: generateEncodedPath(ImportClientRoute.path, params),
});
