import type { RouteDef } from "../route-config";
import { GroupsRoute, GroupsWithIdRoute } from "./routes/Groups";

const routes: RouteDef[] = [GroupsRoute, GroupsWithIdRoute];

export default routes;
