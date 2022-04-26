import type { RouteDef } from "../route-config";
import { AddUserRoute } from "./routes/AddUser";
import { UserRoute } from "./routes/User";
import { UsersRoute } from "./routes/Users";

const routes: RouteDef[] = [AddUserRoute, UsersRoute, UserRoute];

export default routes;
