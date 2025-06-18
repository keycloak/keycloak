import type { AppRouteObject } from "../routes";
import { AddOrganizationRoute } from "./routes/AddOrganization";
import { EditOrganizationRoute } from "./routes/EditOrganization";
import { OrganizationMemberRoleMappingRoute } from "./routes/OrganizationMemberRoleMapping";
import { OrganizationRoleRoute } from "./routes/OrganizationRole";
import { OrganizationsRoute } from "./routes/Organizations";

const routes: AppRouteObject[] = [
  OrganizationsRoute,
  AddOrganizationRoute,
  EditOrganizationRoute,
  OrganizationRoleRoute,
  OrganizationMemberRoleMappingRoute,
];

export default routes;
