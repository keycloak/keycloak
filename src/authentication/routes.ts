import type { RouteDef } from "../route-config";
import { AuthenticationRoute } from "./routes/Authentication";
import { CreateFlowRoute } from "./routes/CreateFlow";
import { FlowRoute } from "./routes/Flow";

const routes: RouteDef[] = [AuthenticationRoute, CreateFlowRoute, FlowRoute];

export default routes;
