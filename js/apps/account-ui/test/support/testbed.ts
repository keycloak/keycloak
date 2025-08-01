import { adminClient } from "./admin-client.ts";
import { DEFAULT_USER } from "./common.ts";

export async function createTestBed(): Promise<string> {
  const realm = await createTestRealm();

  await adminClient.users.create({
    realm,
    ...DEFAULT_USER,
  });

  return realm;
}

async function createTestRealm(): Promise<string> {
  const { realmName } = await adminClient.realms.create({
    realm: crypto.randomUUID(),
    enabled: true,
  });

  return realmName;
}
