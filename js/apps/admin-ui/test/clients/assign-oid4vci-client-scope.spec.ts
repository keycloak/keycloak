import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { createTestBed } from "../support/testbed.ts";
import { login } from "../utils/login.ts";
import { goToClientScopes, goToClients, goToRealm } from "../utils/sidebar.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { toClients } from "../../src/clients/routes/Clients.tsx";
import { createClient, continueNext, save as saveClient } from "./utils.ts";
import {
  assignOptionalOid4vciClientScope,
  createOid4vciClientScope,
  openClientScopeSetupTab,
  skipIfOID4VCIFeatureDisabled,
} from "./assign-oid4vci-client-scope.ts";

test("OIDC client can assign OID4VCI client scopes", async ({ page }) => {
  await using testBed = await createTestBed({
    verifiableCredentialsEnabled: true,
  });

  await login(page, { to: toClients({ realm: testBed.realm }) });
  await goToRealm(page, testBed.realm);
  await skipIfOID4VCIFeatureDisabled();

  const clientScopeName = `oid4vci-scope-${uuid()}`;
  await goToClientScopes(page);
  await createOid4vciClientScope(page, clientScopeName);

  const clientId = `oidc-client-${uuid()}`;
  await goToClients(page);
  await createClient(page, { clientId, protocol: "OpenID Connect" });
  await continueNext(page);
  await saveClient(page);
  await assertNotificationMessage(page, "Client created successfully");

  await goToClients(page);
  await openClientScopeSetupTab(page, clientId);
  await assignOptionalOid4vciClientScope(page, clientScopeName);
});
