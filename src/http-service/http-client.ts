import { KeycloakService } from '../auth/keycloak.service';

type ConfigResolve = (config: RequestInit) => void;

export interface HttpResponse<T = {}> extends Response {
  data?: T;
}

export interface RequestInitWithParams extends RequestInit {
  params?: { [name: string]: string | number };
}

export class AccountServiceError extends Error {
  constructor(message: string) {
    super(message);
  }
}

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class HttpClient {
  private kcSvc: KeycloakService;

  public constructor(keycloakService: KeycloakService) {
    this.kcSvc = keycloakService;
  }

  public async doGet<T>(
    endpoint: string,
    config?: RequestInitWithParams
  ): Promise<HttpResponse<T>> {
    return this.doRequest(endpoint, { ...config, method: 'get' });
  }

  public async doDelete<T>(
    endpoint: string,
    config?: RequestInitWithParams
  ): Promise<HttpResponse<T>> {
    return this.doRequest(endpoint, { ...config, method: 'delete' });
  }

  public async doPost<T>(
    endpoint: string,
    body: string | {},
    config?: RequestInitWithParams
  ): Promise<HttpResponse<T>> {
    return this.doRequest(endpoint, {
      ...config,
      body: JSON.stringify(body),
      method: 'post',
    });
  }

  public async doPut<T>(
    endpoint: string,
    body: string | {},
    config?: RequestInitWithParams
  ): Promise<HttpResponse<T>> {
    return this.doRequest(endpoint, {
      ...config,
      body: JSON.stringify(body),
      method: 'put',
    });
  }

  public async doRequest<T>(
    endpoint: string,
    config?: RequestInitWithParams
  ): Promise<HttpResponse<T>> {
    const response: HttpResponse<T> = await fetch(
      this.makeUrl(endpoint, config).toString(),
      await this.makeConfig(config)
    );

    try {
      response.data = await response.json();
    } catch (e) {
      console.warn(e);
    }

    if (!response.ok) {
      this.handleError(response);
    }

    return response;
  }

  private handleError(response: HttpResponse): void {
    if (response != null && response.status === 401) {
      // session timed out?
      this.kcSvc.login();
    }

    if (response != null && response.data != null) {
      throw new AccountServiceError((response.data as any).errorMessage);
    } else {
      throw new AccountServiceError(response.statusText);
    }
  }

  private makeUrl(url: string, config?: RequestInitWithParams): string {
    const searchParams = new URLSearchParams();
    // add request params
    if (config && {}.hasOwnProperty.call(config, 'params')) {
      const params: { [name: string]: string } = (config.params as {}) || {};
      Object.keys(params).forEach((key) =>
        searchParams.append(key, params[key])
      );
    }

    return url + '?' + searchParams.toString();
  }

  private makeConfig(config: RequestInit = {}): Promise<RequestInit> {
    return new Promise((resolve: ConfigResolve) => {
      this.kcSvc
        .getToken()
        .then((token: string) => {
          resolve({
            ...config,
            headers: {
              'Content-Type': 'application/json',
              ...config.headers,
              Authorization: 'Bearer ' + token,
            },
          });
        })
        .catch(() => {
          this.kcSvc.login();
        });
    });
  }
}

window.addEventListener(
  'unhandledrejection',
  (event: PromiseRejectionEvent) => {
    event.promise.catch((error) => {
      if (error instanceof AccountServiceError) {
        // We already handled the error. Ignore unhandled rejection.
        event.preventDefault();
      }
    });
  }
);
