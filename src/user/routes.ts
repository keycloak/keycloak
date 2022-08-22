import type { RouteDef } from "../route-config";
import { AddUserRoute } from "./routes/AddUser";
import { UserRoute } from "./routes/User";
import { UsersRoute, UsersRouteWithTab } from "./routes/Users";

const routes: RouteDef[] = [
  AddUserRoute,
  UsersRoute,
  UsersRouteWithTab,
  UserRoute,
];

export default routes;
