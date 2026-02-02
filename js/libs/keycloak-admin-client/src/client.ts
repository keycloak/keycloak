import type { RequestArgs } from "./resources/agent.js";
import { AttackDetection } from "./resources/attackDetection.js";
import { AuthenticationManagement } from "./resources/authenticationManagement.js";
import { Cache } from "./resources/cache.js";
import { ClientPolicies } from "./resources/clientPolicies.js";
import { Clients } from "./resources/clients.js";
import { ClientScopes } from "./resources/clientScopes.js";
import { Components } from "./resources/components.js";
import { Groups } from "./resources/groups.js";
import { IdentityProviders } from "./resources/identityProviders.js";
import { Realms } from "./resources/realms.js";
import { Organizations } from "./resources/organizations.js";
import { Workflows } from "./resources/workflows.js";
import { Roles } from "./resources/roles.js";
import { ServerInfo } from "./resources/serverInfo.js";
import { Users } from "./resources/users.js";
import { UserStorageProvider } from "./resources/userStorageProvider.js";
import { WhoAmI } from "./resources/whoAmI.js";
import { Credentials, getToken, Settings } from "./utils/auth.js";
import { defaultBaseUrl, defaultRealm } from "./utils/constants.js";
import { DecodedToken, decodeToken } from "./utils/decode.js";

export type RequestOptions = Omit<RequestInit, "signal">;

export interface TokenProvider {
  getAccessToken: () => Promise<string | undefined>;
}

export interface ConnectionConfig {
  baseUrl?: string;
  realmName?: string;
  requestOptions?: RequestOptions;
  requestArgOptions?: Pick<RequestArgs, "catchNotFound">;
  timeout?: number;
}

const MIN_VALIDITY = 5; // in seconds

export class KeycloakAdminClient {
  // Resources
  public users: Users;
  public userStorageProvider: UserStorageProvider;
  public groups: Groups;
  public roles: Roles;
  public organizations: Organizations;
  public workflows: Workflows;
  public clients: Clients;
  public realms: Realms;
  public clientScopes: ClientScopes;
  public clientPolicies: ClientPolicies;
  public identityProviders: IdentityProviders;
  public components: Components;
  public serverInfo: ServerInfo;
  public whoAmI: WhoAmI;
  public attackDetection: AttackDetection;
  public authenticationManagement: AuthenticationManagement;
  public cache: Cache;

  // Members
  public baseUrl: string;
  public realmName: string;
  public scope?: string;
  public accessToken?: string;
  public refreshToken?: string;
  public timeout?: number;

  #requestOptions?: RequestOptions;
  #globalRequestArgOptions?: Pick<RequestArgs, "catchNotFound">;
  #tokenProvider?: TokenProvider;
  #accessTokenDecoded?: DecodedToken;
  #refreshTokenDecoded?: DecodedToken;
  #credentials?: Credentials;

  constructor(connectionConfig?: ConnectionConfig) {
    this.baseUrl = connectionConfig?.baseUrl || defaultBaseUrl;
    this.realmName = connectionConfig?.realmName || defaultRealm;
    this.timeout = connectionConfig?.timeout;
    this.#requestOptions = connectionConfig?.requestOptions;
    this.#globalRequestArgOptions = connectionConfig?.requestArgOptions;

    // Initialize resources
    this.users = new Users(this);
    this.userStorageProvider = new UserStorageProvider(this);
    this.groups = new Groups(this);
    this.roles = new Roles(this);
    this.organizations = new Organizations(this);
    this.workflows = new Workflows(this);
    this.clients = new Clients(this);
    this.realms = new Realms(this);
    this.clientScopes = new ClientScopes(this);
    this.clientPolicies = new ClientPolicies(this);
    this.identityProviders = new IdentityProviders(this);
    this.components = new Components(this);
    this.authenticationManagement = new AuthenticationManagement(this);
    this.serverInfo = new ServerInfo(this);
    this.whoAmI = new WhoAmI(this);
    this.attackDetection = new AttackDetection(this);
    this.cache = new Cache(this);
  }

  public async auth(credentials: Credentials) {
    const { accessToken, refreshToken } = await getToken(
      this.#getTokenSettings(credentials),
    );
    this.#credentials = credentials;
    this.setAccessToken(accessToken);
    this.setRefreshToken(refreshToken);
  }

  #getTokenSettings(credentials: Credentials): Settings {
    return {
      baseUrl: this.baseUrl,
      realmName: this.realmName,
      scope: this.scope,
      credentials,
      requestOptions: {
        ...this.#requestOptions,
        ...(this.timeout ? { signal: AbortSignal.timeout(this.timeout) } : {}),
      },
    };
  }

  public registerTokenProvider(provider: TokenProvider) {
    if (this.#tokenProvider) {
      throw new Error("An existing token provider was already registered.");
    }

    this.#tokenProvider = provider;
  }

  public setAccessToken(token: string) {
    this.accessToken = token;
    this.#accessTokenDecoded = decodeToken(token);
  }

  public setRefreshToken(token: string) {
    this.refreshToken = token;
    this.#refreshTokenDecoded = decodeToken(token);
  }

  public async getAccessToken() {
    if (this.#tokenProvider) {
      return this.#tokenProvider.getAccessToken();
    }

    if (this.isTokenExpired()) {
      await this.#refreshAccessToken();
    }

    return this.accessToken;
  }

  async #refreshAccessToken() {
    if (!this.refreshToken || !this.#credentials) {
      throw new Error(
        "Cannot refresh token: missing refresh token or credentials",
      );
    }

    if (this.isRefreshTokenExpired()) {
      throw new Error("Cannot refresh token: refresh token has expired");
    }

    const { accessToken, refreshToken } = await getToken(
      this.#getTokenSettings({
        grantType: "refresh_token",
        clientId: this.#credentials.clientId,
        clientSecret: this.#credentials.clientSecret,
        refreshToken: this.refreshToken,
      }),
    );

    this.setAccessToken(accessToken);
    this.setRefreshToken(refreshToken);
  }

  public isTokenExpired(): boolean {
    return this.#isExpired(this.#accessTokenDecoded);
  }

  public isRefreshTokenExpired(): boolean {
    return this.#isExpired(this.#refreshTokenDecoded);
  }

  #isExpired(token?: DecodedToken): boolean {
    if (typeof token?.exp !== "number") {
      return false;
    }
    const expiresIn =
      token.exp - Math.ceil(new Date().getTime() / 1000) - MIN_VALIDITY;
    return expiresIn < 0;
  }

  public getRequestOptions() {
    return this.#requestOptions;
  }

  public getGlobalRequestArgOptions():
    | Pick<RequestArgs, "catchNotFound">
    | undefined {
    return this.#globalRequestArgOptions;
  }

  public setConfig(connectionConfig: ConnectionConfig) {
    if (
      typeof connectionConfig.baseUrl === "string" &&
      connectionConfig.baseUrl
    ) {
      this.baseUrl = connectionConfig.baseUrl;
    }

    if (
      typeof connectionConfig.realmName === "string" &&
      connectionConfig.realmName
    ) {
      this.realmName = connectionConfig.realmName;
    }
    this.#requestOptions = connectionConfig.requestOptions;
  }
}
