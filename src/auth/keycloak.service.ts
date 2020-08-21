import { KeycloakLoginOptions } from 'keycloak-js';
import { useTranslation } from 'react-i18next';

export type KeycloakClient = Keycloak.KeycloakInstance;

type Token = {
  given_name: string;
  family_name: string;
  preferred_username: string;
};

export class KeycloakService {
  private keycloakAuth: KeycloakClient;

  public constructor(keycloak: KeycloakClient) {
    this.keycloakAuth = keycloak;
  }

  public authenticated(): boolean {
    return this.keycloakAuth.authenticated
      ? this.keycloakAuth.authenticated
      : false;
  }

  public login(options?: KeycloakLoginOptions): void {
    this.keycloakAuth.login(options);
  }

  public logout(redirectUri: string = ''): void {
    this.keycloakAuth.logout({ redirectUri: redirectUri });
  }

  public account(): void {
    this.keycloakAuth.accountManagement();
  }

  public authServerUrl(): string | undefined {
    const authServerUrl = this.keycloakAuth.authServerUrl;
    return authServerUrl!.charAt(authServerUrl!.length - 1) === '/'
      ? authServerUrl
      : authServerUrl + '/';
  }

  public realm(): string | undefined {
    return this.keycloakAuth.realm;
  }

  public get loggedInUser(): string {
    const { t } = useTranslation();
    return this.loggedInUserName(t, this.keycloakAuth.tokenParsed as Token);
  }

  private loggedInUserName = (t: Function, tokenParsed: Token) => {
    let userName = t('unknownUser');
    if (tokenParsed) {
      const givenName = tokenParsed.given_name;
      const familyName = tokenParsed.family_name;
      const preferredUsername = tokenParsed.preferred_username;
      if (givenName && familyName) {
        userName = t('fullName', { givenName, familyName });
      } else {
        userName = givenName || familyName || preferredUsername || userName;
      }
    }
    return userName;
  };

  public getToken(): Promise<string> {
    return new Promise<string>((resolve, reject) => {
      if (this.keycloakAuth.token) {
        this.keycloakAuth
          .updateToken(5)
          .then(() => {
            resolve(this.keycloakAuth.token as string);
          })
          .catch(() => {
            reject('Failed to refresh token');
          });
      } else {
        reject('Not logged in');
      }
    });
  }
}
