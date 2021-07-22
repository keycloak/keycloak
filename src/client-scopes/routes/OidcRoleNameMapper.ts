import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { RoleMappingForm } from "../add/RoleMappingForm";

export type OidcRoleNameMapperParams = {
  realm: string;
  id: string;
};

export const OidcRoleNameMapperRoute: RouteDef = {
  path: "/:realm/client-scopes/:id/mappers/oidc-role-name-mapper",
  component: RoleMappingForm,
  breadcrumb: (t) => t("common:mappingDetails"),
  access: "view-clients",
};

export const toOidcRoleNameMapper = (
  params: OidcRoleNameMapperParams
): LocationDescriptorObject => ({
  pathname: generatePath(OidcRoleNameMapperRoute.path, params),
});
