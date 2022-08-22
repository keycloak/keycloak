import type { RouteDef } from "../route-config";
import { GroupsRoute, GroupsWithIdRoute } from "./routes/Groups";
import { GroupsSearchRoute } from "./routes/GroupsSearch";

const routes: RouteDef[] = [GroupsSearchRoute, GroupsRoute, GroupsWithIdRoute];

export default routes;
