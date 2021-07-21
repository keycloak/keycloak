import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ImportForm } from "../import/ImportForm";

export type ImportClientParams = { realm: string };

export const ImportClientRoute: RouteDef = {
  path: "/:realm/clients/import-client",
  component: ImportForm,
  breadcrumb: (t) => t("clients:importClient"),
  access: "manage-clients",
};

export const toImportClient = (
  params: ImportClientParams
): LocationDescriptorObject => ({
  pathname: generatePath(ImportClientRoute.path, params),
});
