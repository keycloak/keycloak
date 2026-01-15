import { FetchRequestAdapter } from "@microsoft/kiota-http-fetchlibrary";
import {
  type AccessTokenProvider,
  AllowedHostsValidator,
  BaseBearerTokenAuthenticationProvider,
} from "@microsoft/kiota-abstractions";
import { createAdminClient } from "../src/adminClient.js";
import type { OIDCClientRepresentation } from "../src/models/index.js";

// Configuration
const KEYCLOAK_URL = process.env.KEYCLOAK_URL || "http://localhost:8080";
const REALM = process.env.KEYCLOAK_REALM || "master";
const CLIENT_ID = process.env.KEYCLOAK_CLIENT_ID || "admin-cli";
const USERNAME = process.env.KEYCLOAK_USERNAME || "admin";
const PASSWORD = process.env.KEYCLOAK_PASSWORD || "admin";

interface TokenResponse {
  access_token: string;
  expires_in: number;
  refresh_token?: string;
  token_type: string;
}

/**
 * Simple access token provider that fetches tokens from Keycloak
 */
class KeycloakAccessTokenProvider implements AccessTokenProvider {
  #accessToken: string | null = null;
  #tokenExpiry: number = 0;
  #allowedHostsValidator: AllowedHostsValidator;
  #keycloakUrl: string;
  #realm: string;
  #clientId: string;
  #username: string;
  #password: string;

  constructor(
    keycloakUrl: string,
    realm: string,
    clientId: string,
    username: string,
    password: string,
  ) {
    this.#keycloakUrl = keycloakUrl;
    this.#realm = realm;
    this.#clientId = clientId;
    this.#username = username;
    this.#password = password;
    this.#allowedHostsValidator = new AllowedHostsValidator(
      new Set([new URL(keycloakUrl).host]),
    );
  }

  async getAuthorizationToken(): Promise<string> {
    // Check if we have a valid cached token
    if (this.#accessToken && Date.now() < this.#tokenExpiry - 30000) {
      return this.#accessToken;
    }

    // Fetch a new token
    const tokenUrl = `${this.#keycloakUrl}/realms/${this.#realm}/protocol/openid-connect/token`;
    const response = await fetch(tokenUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({
        grant_type: "password",
        client_id: this.#clientId,
        username: this.#username,
        password: this.#password,
      }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(
        `Failed to get access token: ${response.status} - ${error}`,
      );
    }

    const tokenData: TokenResponse = (await response.json()) as TokenResponse;
    this.#accessToken = tokenData.access_token;
    this.#tokenExpiry = Date.now() + tokenData.expires_in * 1000;

    return this.#accessToken;
  }

  getAllowedHostsValidator(): AllowedHostsValidator {
    return this.#allowedHostsValidator;
  }
}

async function main() {
  console.log("🔐 Keycloak Admin Client v2 - Test Application\n");
  console.log(`Keycloak URL: ${KEYCLOAK_URL}`);
  console.log(`Realm: ${REALM}`);

  // Create the authentication provider
  const tokenProvider = new KeycloakAccessTokenProvider(
    KEYCLOAK_URL,
    REALM,
    CLIENT_ID,
    USERNAME,
    PASSWORD,
  );
  const authProvider = new BaseBearerTokenAuthenticationProvider(tokenProvider);

  // Create the request adapter
  const adapter = new FetchRequestAdapter(authProvider);
  adapter.baseUrl = KEYCLOAK_URL;

  // Create the admin client
  const client = createAdminClient(adapter);

  try {
    // List all clients in the realm
    console.log(`📋 Listing clients in realm '${REALM}'...\n`);
    const clients = await client.admin.api.v2.realms
      .byName(REALM)
      .clients.get();

    if (clients && clients.length > 0) {
      console.log(`Found ${clients.length} client(s):\n`);
      for (const c of clients) {
        console.log(`  - ${c.clientId} (${c.protocol || "unknown protocol"})`);
        if (c.displayName) {
          console.log(`    Display Name: ${c.displayName}`);
        }
        if (c.description) {
          console.log(`    Description: ${c.description}`);
        }
        console.log(`    Enabled: ${c.enabled ?? "unknown"}`);
        console.log();
      }
    } else {
      console.log("No clients found.");
    }

    // Create a new test client
    console.log("\n🆕 Creating a new OIDC test client...\n");
    const newClient: OIDCClientRepresentation = {
      clientId: `test-client-${Date.now()}`,
      displayName: "Test Client",
      description: "A test client created by the admin client v2",
      enabled: true,
      protocol: "openid-connect",
      redirectUris: ["http://localhost:3000/callback"],
      webOrigins: ["http://localhost:3000"],
    };

    await client.admin.api.v2.realms.byName(REALM).clients.post(newClient);
    console.log(`✅ Created client: ${newClient.clientId}`);

    // List clients again to verify
    console.log("\n📋 Listing clients again to verify...\n");
    const updatedClients = await client.admin.api.v2.realms
      .byName(REALM)
      .clients.get();
    const createdClient = updatedClients?.find(
      (c) => c.clientId === newClient.clientId,
    );

    if (createdClient) {
      console.log(`✅ Verified: Client '${createdClient.clientId}' exists!`);
    } else {
      console.log(`❌ Could not find the created client`);
    }

    console.log("\n🎉 Test completed successfully!");
  } catch (error) {
    console.error("❌ Error:", error);
    process.exit(1);
  }
}

void main();
