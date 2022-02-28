/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as React from "../../common/keycloak/web_modules/react.js";
import * as ReactDOM from "../../common/keycloak/web_modules/react-dom.js";
import { HashRouter } from "../../common/keycloak/web_modules/react-router-dom.js";
import { App } from "./App.js";
import { flattenContent, initGroupAndItemIds, isExpansion, isModulePageDef } from "./ContentPages.js";
import { KeycloakService } from "./keycloak-service/keycloak.service.js";
import { KeycloakContext } from "./keycloak-service/KeycloakContext.js";
import { AccountServiceClient } from "./account-service/account.service.js";
import { AccountServiceContext } from "./account-service/AccountServiceContext.js";
export class Main extends React.Component {
  constructor(props) {
    super(props);
  }

  componentDidMount() {
    isReactLoading = false;
    toggleReact();
  }

  render() {
    const keycloakService = new KeycloakService(keycloak);
    return React.createElement(HashRouter, null, React.createElement(KeycloakContext.Provider, {
      value: keycloakService
    }, React.createElement(AccountServiceContext.Provider, {
      value: new AccountServiceClient(keycloakService)
    }, React.createElement(App, null))));
  }

}
;
const e = React.createElement;

function removeHidden(items) {
  const visible = [];

  for (let item of items) {
    if (item.hidden && eval(item.hidden)) continue;

    if (isExpansion(item)) {
      visible.push(item);
      item.content = removeHidden(item.content);

      if (item.content.length === 0) {
        visible.pop(); // remove empty expansion
      }
    } else {
      visible.push(item);
    }
  }

  return visible;
}

content = removeHidden(content);
initGroupAndItemIds();

function loadModule(modulePage) {
  return new Promise((resolve, reject) => {
    console.log('loading: ' + resourceUrl + modulePage.modulePath);
    import(resourceUrl + modulePage.modulePath).then(module => {
      modulePage.module = module;
      resolve(modulePage);
    }).catch(error => {
      console.warn('Unable to load ' + modulePage.label + ' because ' + error.message);
      reject(modulePage);
    });
  });
}

;
const moduleLoaders = [];
flattenContent(content).forEach(item => {
  if (isModulePageDef(item)) {
    moduleLoaders.push(loadModule(item));
  }
}); // load content modules and start

Promise.all(moduleLoaders).then(() => {
  const domContainer = document.querySelector('#main_react_container');
  ReactDOM.render(e(Main), domContainer);
});
//# sourceMappingURL=Main.js.map