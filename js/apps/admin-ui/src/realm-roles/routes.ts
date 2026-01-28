import type { AppRouteObject } from "../routes";
import { AddRoleRoute } from "./routes/AddRole";
import { RealmRoleRoute } from "./routes/RealmRole";
import { RealmRolesRoute } from "./routes/RealmRoles";

const routes: AppRouteObject[] = [
  RealmRolesRoute,
  AddRoleRoute,
  RealmRoleRoute,
];

export default routes;
