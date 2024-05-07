import type { AppRouteObject } from "../routes";
import { AddUserRoute } from "./routes/AddUser";
import { UserRoute } from "./routes/User";
import { UsersRoute, UsersRouteWithTab } from "./routes/Users";
import {
  CreateCustomAttributeStoreInstanceRoute,
  UpdateCustomAttributeStoreInstanceRoute,
  UpdateCustomAttributeStoreInstanceRouteWithTab,
} from "../components/attribute-store-tab/routes/CustomInstance";
import { AttributeStorePatchRoute } from "../components/attribute-store-tab/routes/AttributeStorePatch";

const routes: AppRouteObject[] = [
  AddUserRoute,
  UsersRoute,
  UsersRouteWithTab,
  UserRoute,
  CreateCustomAttributeStoreInstanceRoute,
  UpdateCustomAttributeStoreInstanceRoute,
  UpdateCustomAttributeStoreInstanceRouteWithTab,
  AttributeStorePatchRoute,
];

export default routes;
