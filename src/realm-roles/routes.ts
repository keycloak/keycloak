import type { RouteDef } from "../route-config";
import { AddRoleRoute } from "./routes/AddRole";
import { AddRoleToClientRoute } from "./routes/AddRoleToClient";
import { ClientRoleRoute } from "./routes/ClientRole";
import { RealmRoleRoute } from "./routes/RealmRole";
import { RealmRolesRoute } from "./routes/RealmRoles";

const routes: RouteDef[] = [
  AddRoleToClientRoute,
  ClientRoleRoute,
  RealmRolesRoute,
  AddRoleRoute,
  RealmRoleRoute,
];

export default routes;
