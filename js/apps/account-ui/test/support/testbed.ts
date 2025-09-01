import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import { adminClient } from "./admin-client.ts";
import { DEFAULT_USER } from "./common.ts";

export async function createTestBed(
  overrides?: RealmRepresentation,
): Promise<string> {
  const { realmName } = await adminClient.realms.create({
    enabled: true,
    users: [DEFAULT_USER],
    ...overrides,
    realm: crypto.randomUUID(),
  });

  return realmName;
}
