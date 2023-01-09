import type { RouteDef } from "../route-config";
import { AddRoleRoute } from "./routes/AddRole";
import { ClientRoleRoute, ClientRoleRouteWithTab } from "./routes/ClientRole";
import { RealmRoleRoute, RealmRoleRouteWithTab } from "./routes/RealmRole";
import { RealmRolesRoute } from "./routes/RealmRoles";

const routes: RouteDef[] = [
  ClientRoleRoute,
  ClientRoleRouteWithTab,
  RealmRolesRoute,
  AddRoleRoute,
  RealmRoleRoute,
  RealmRoleRouteWithTab,
];

export default routes;
