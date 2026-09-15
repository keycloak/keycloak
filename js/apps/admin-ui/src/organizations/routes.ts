import type { AppRouteObject } from "../routes";
import { AddOrganizationRoute } from "./routes/AddOrganization";
import { EditOrganizationRoute } from "./routes/EditOrganization";
import { OrganizationsRoute } from "./routes/Organizations";
import { OrganizationRoleRoute } from "./routes/OrganizationRole";

const routes: AppRouteObject[] = [
  OrganizationsRoute,
  AddOrganizationRoute,
  EditOrganizationRoute,
  OrganizationRoleRoute,
];

export default routes;
