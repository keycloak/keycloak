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
import * as React from "../../../../common/keycloak/web_modules/react.js";
import { DataList, DataListItem, DataListItemRow, DataListCell, DataListItemCells, ChipGroup, ChipGroupToolbarItem, Chip } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { RepositoryIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { Msg } from "../../widgets/Msg.js";
import { AbstractResourcesTable } from "./AbstractResourceTable.js";
import EmptyMessageState from "../../widgets/EmptyMessageState.js";
export class SharedResourcesTable extends AbstractResourcesTable {
  constructor(props) {
    super(props);
    this.state = {
      permissions: new Map()
    };
  }

  render() {
    if (this.props.resources.data.length === 0) {
      return React.createElement(EmptyMessageState, {
        icon: RepositoryIcon,
        messageKey: "noResourcesSharedWithYou"
      });
    }

    return React.createElement(DataList, {
      "aria-label": Msg.localize('resources'),
      id: "sharedResourcesList"
    }, React.createElement(DataListItem, {
      key: "resource-header",
      "aria-labelledby": "resource-header"
    }, React.createElement(DataListItemRow, null, React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, {
        key: "resource-name-header",
        width: 2
      }, React.createElement("strong", null, React.createElement(Msg, {
        msgKey: "resourceName"
      }))), React.createElement(DataListCell, {
        key: "application-name-header",
        width: 2
      }, React.createElement("strong", null, React.createElement(Msg, {
        msgKey: "application"
      }))), React.createElement(DataListCell, {
        key: "permission-header",
        width: 2
      }), React.createElement(DataListCell, {
        key: "requests-header",
        width: 2
      })]
    }))), this.props.resources.data.map((resource, row) => React.createElement(DataListItem, {
      key: 'resource-' + row,
      "aria-labelledby": resource.name
    }, React.createElement(DataListItemRow, null, React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, {
        key: 'resourceName-' + row,
        width: 2
      }, React.createElement(Msg, {
        msgKey: resource.name
      })), React.createElement(DataListCell, {
        key: 'resourceClient-' + row,
        width: 2
      }, React.createElement("a", {
        href: resource.client.baseUrl
      }, this.getClientName(resource.client))), React.createElement(DataListCell, {
        key: 'permissions-' + row,
        width: 2
      }, resource.scopes.length > 0 && React.createElement(ChipGroup, {
        withToolbar: true
      }, React.createElement(ChipGroupToolbarItem, {
        key: "permissions",
        categoryName: Msg.localize('permissions')
      }, resource.scopes.map(scope => React.createElement(Chip, {
        key: scope.name,
        isReadOnly: true
      }, scope.displayName || scope.name))))), React.createElement(DataListCell, {
        key: 'pending-' + row,
        width: 2
      }, resource.shareRequests.length > 0 && React.createElement(ChipGroup, {
        withToolbar: true
      }, React.createElement(ChipGroupToolbarItem, {
        key: "permissions",
        categoryName: Msg.localize('pending')
      }, resource.shareRequests[0].scopes.map(scope => React.createElement(Chip, {
        key: scope.name,
        isReadOnly: true
      }, scope.displayName || scope.name)))))]
    })))));
  }

}
//# sourceMappingURL=SharedResourcesTable.js.map