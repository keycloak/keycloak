import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import { adminClient } from "./admin-client.ts";
import { DEFAULT_USER } from "./common.ts";

export interface TestBed extends AsyncDisposable {
  realm: string;
}

export async function createTestBed(
  overrides?: RealmRepresentation,
): Promise<TestBed> {
  const { realmName: realm } = await adminClient.realms.create({
    enabled: true,
    users: [DEFAULT_USER],
    ...overrides,
    realm: crypto.randomUUID(),
  });

  const deleteRealm = () => adminClient.realms.del({ realm });

  return {
    realm,
    [Symbol.asyncDispose]: deleteRealm,
  };
}
