import type { AppRouteObject } from "../routes";
import { GroupsRoute, GroupsWithIdRoute } from "./routes/Groups";

const routes: AppRouteObject[] = [GroupsRoute, GroupsWithIdRoute];

export default routes;
