import type { RouteDef } from "../route-config";
import { GroupsRoute } from "./routes/Groups";
import { GroupsSearchRoute } from "./routes/GroupsSearch";

const routes: RouteDef[] = [GroupsSearchRoute, GroupsRoute];

export default routes;
