import type { RouteDef } from "../route-config";
import { AddRoleRoute } from "./routes/AddRole";
import { RealmRoleRoute } from "./routes/RealmRole";
import { RealmRolesRoute } from "./routes/RealmRoles";

const routes: RouteDef[] = [RealmRolesRoute, AddRoleRoute, RealmRoleRoute];

export default routes;
