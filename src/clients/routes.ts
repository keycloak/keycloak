import type { RouteDef } from "../route-config";
import { AddClientRoute } from "./routes/AddClient";
import { ClientRoute } from "./routes/Client";
import { ClientsRoute } from "./routes/Clients";
import { CreateInitialAccessTokenRoute } from "./routes/CreateInitialAccessToken";
import { ImportClientRoute } from "./routes/ImportClient";

const routes: RouteDef[] = [
  AddClientRoute,
  ImportClientRoute,
  ClientsRoute,
  CreateInitialAccessTokenRoute,
  ClientRoute,
];

export default routes;
