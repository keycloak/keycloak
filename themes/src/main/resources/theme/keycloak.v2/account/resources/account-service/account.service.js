function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

/*
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import { ContentAlert } from "../content/ContentAlert.js";
export class AccountServiceError extends Error {
  constructor(response) {
    super(response.statusText);
    this.response = response;
  }

}
/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */

export class AccountServiceClient {
  constructor(keycloakService) {
    _defineProperty(this, "kcSvc", void 0);

    _defineProperty(this, "accountUrl", void 0);

    this.kcSvc = keycloakService;
    this.accountUrl = this.kcSvc.authServerUrl() + 'realms/' + this.kcSvc.realm() + '/account';
  }

  async doGet(endpoint, config) {
    return this.doRequest(endpoint, { ...config,
      method: 'get'
    });
  }

  async doDelete(endpoint, config) {
    return this.doRequest(endpoint, { ...config,
      method: 'delete'
    });
  }

  async doPost(endpoint, body, config) {
    return this.doRequest(endpoint, { ...config,
      body: JSON.stringify(body),
      method: 'post'
    });
  }

  async doPut(endpoint, body, config) {
    return this.doRequest(endpoint, { ...config,
      body: JSON.stringify(body),
      method: 'put'
    });
  }

  async doRequest(endpoint, config) {
    const response = await fetch(this.makeUrl(endpoint, config).toString(), await this.makeConfig(config));

    try {
      response.data = await response.json();
    } catch (e) {} // ignore.  Might be empty


    if (!response.ok) {
      this.handleError(response);
      throw new AccountServiceError(response);
    }

    return response;
  }

  handleError(response) {
    if (response !== null && response.status === 401) {
      if (this.kcSvc.authenticated() && !this.kcSvc.audiencePresent()) {
        // authenticated and the audience is not present => not allowed
        window.location.href = baseUrl + '#/forbidden';
      } else {
        // session timed out?
        this.kcSvc.login();
      }
    }

    if (response !== null && response.status === 403) {
      window.location.href = baseUrl + '#/forbidden';
    }

    if (response !== null && response.data != null) {
      if (response.data['errors'] != null) {
        for (let err of response.data['errors']) ContentAlert.danger(err['errorMessage'], err['params']);
      } else {
        ContentAlert.danger(`${response.statusText}: ${response.data['errorMessage'] ? response.data['errorMessage'] : ''} ${response.data['error'] ? response.data['error'] : ''}`);
      }

      ;
    } else {
      ContentAlert.danger(response.statusText);
    }
  }

  makeUrl(endpoint, config) {
    if (endpoint.startsWith('http')) return new URL(endpoint);
    const url = new URL(this.accountUrl + endpoint); // add request params

    if (config && config.hasOwnProperty('params')) {
      const params = config.params || {};
      Object.keys(params).forEach(key => url.searchParams.append(key, params[key]));
    }

    return url;
  }

  makeConfig(config = {}) {
    return new Promise(resolve => {
      this.kcSvc.getToken().then(token => {
        resolve({ ...config,
          headers: {
            'Content-Type': 'application/json',
            ...config.headers,
            Authorization: 'Bearer ' + token
          }
        });
      }).catch(() => {
        this.kcSvc.login();
      });
    });
  }

}
window.addEventListener("unhandledrejection", event => {
  event.promise.catch(error => {
    if (error instanceof AccountServiceError) {
      // We already handled the error. Ignore unhandled rejection.
      event.preventDefault();
    }
  });
});
//# sourceMappingURL=account.service.js.map