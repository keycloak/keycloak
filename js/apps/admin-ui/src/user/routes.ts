import type { AppRouteObject } from "../routes";
import { AddAdminUserRoute, AddUserRoute } from "./routes/AddUser";
import { UserRoute } from "./routes/User";
import { UsersRoute, UsersRouteWithTab } from "./routes/Users";

const routes: AppRouteObject[] = [
  AddUserRoute,
  AddAdminUserRoute,
  UsersRoute,
  UsersRouteWithTab,
  UserRoute,
];

export default routes;
