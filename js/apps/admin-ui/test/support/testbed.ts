import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import adminClient from "../utils/AdminClient.ts";

export interface TestBed extends AsyncDisposable {
  realm: string;
}

export async function createTestBed(
  overrides?: RealmRepresentation,
): Promise<TestBed> {
  const { realmName: realm } = await adminClient.createRealm(
    crypto.randomUUID(),
    overrides,
  );

  const deleteRealm = () => adminClient.deleteRealm(realm);

  return {
    realm,
    [Symbol.asyncDispose]: deleteRealm,
  };
}
