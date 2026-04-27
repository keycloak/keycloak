import type { AppRouteObject } from "../routes";
import { AddOrganizationRoute } from "./routes/AddOrganization";
import { EditOrganizationRoute } from "./routes/EditOrganization";
import { OrganizationsRoute } from "./routes/Organizations";

const routes: AppRouteObject[] = [
  OrganizationsRoute,
  AddOrganizationRoute,
  EditOrganizationRoute,
];

export default routes;
