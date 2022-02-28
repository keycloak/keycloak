function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

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
import { withRouter } from "../../../../common/keycloak/web_modules/react-router-dom.js";
import { Button, DataList, DataListAction, DataListItemCells, DataListCell, DataListItem, DataListItemRow, Stack, StackItem, Title, TitleLevel, DataListActionVisibility, Dropdown, DropdownPosition, KebabToggle } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { AIACommand } from "../../util/AIACommand.js";
import TimeUtil from "../../util/TimeUtil.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { ContinueCancelModal } from "../../widgets/ContinueCancelModal.js";
import { Msg } from "../../widgets/Msg.js";
import { ContentPage } from "../ContentPage.js";
import { ContentAlert } from "../ContentAlert.js";
import { KeycloakContext } from "../../keycloak-service/KeycloakContext.js";
import { css } from "../../../../common/keycloak/web_modules/@patternfly/react-styles.js";

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
class SigningInPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "handleRemove", (credentialId, userLabel) => {
      this.context.doDelete("/credentials/" + credentialId).then(() => {
        this.getCredentialContainers();
        ContentAlert.success('successRemovedMessage', [userLabel]);
      });
    });

    this.context = context;
    this.state = {
      credentialContainers: new Map(),
      toggle: false
    };
    this.getCredentialContainers();
  }

  getCredentialContainers() {
    this.context.doGet("/credentials").then(response => {
      const allContainers = new Map();
      const containers = response.data || [];
      containers.forEach(container => {
        let categoryMap = allContainers.get(container.category);

        if (!categoryMap) {
          categoryMap = new Map();
          allContainers.set(container.category, categoryMap);
        }

        categoryMap.set(container.type, container);
      });
      this.setState({
        credentialContainers: allContainers
      });
      console.log({
        allContainers
      });
    });
  }

  static credElementId(credType, credId, item) {
    return `${credType}-${item}-${credId.substring(0, 8)}`;
  }

  render() {
    return React.createElement(ContentPage, {
      title: "signingIn",
      introMessage: "signingInSubMessage"
    }, React.createElement(Stack, {
      gutter: "md"
    }, this.renderCategories()));
  }

  renderCategories() {
    return React.createElement(React.Fragment, null, " ", Array.from(this.state.credentialContainers.keys()).map(category => React.createElement(StackItem, {
      key: category,
      isFilled: true
    }, React.createElement(Title, {
      id: `${category}-categ-title`,
      headingLevel: TitleLevel.h2,
      size: "2xl"
    }, React.createElement("strong", null, React.createElement(Msg, {
      msgKey: category
    }))), React.createElement(DataList, {
      "aria-label": "foo"
    }, this.renderTypes(this.state.credentialContainers.get(category))))));
  }

  renderTypes(credTypeMap) {
    return React.createElement(KeycloakContext.Consumer, null, keycloak => React.createElement(React.Fragment, null, Array.from(credTypeMap.keys()).map((credType, index, typeArray) => [this.renderCredTypeTitle(credTypeMap.get(credType), keycloak), this.renderUserCredentials(credTypeMap, credType, keycloak), this.renderEmptyRow(credTypeMap.get(credType).type, index === typeArray.length - 1)])));
  }

  renderEmptyRow(type, isLast) {
    if (isLast) return; // don't put empty row at the end

    return React.createElement(DataListItem, {
      "aria-labelledby": 'empty-list-item-' + type
    }, React.createElement(DataListItemRow, {
      key: 'empty-row-' + type
    }, React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, null)]
    })));
  }

  renderUserCredentials(credTypeMap, credType, keycloak) {
    const credContainer = credTypeMap.get(credType);
    const userCredentials = credContainer.userCredentials;
    const removeable = credContainer.removeable;
    const type = credContainer.type;
    const displayName = credContainer.displayName;

    if (!userCredentials || userCredentials.length === 0) {
      const localizedDisplayName = Msg.localize(displayName);
      return React.createElement(DataListItem, {
        key: "no-credentials-list-item",
        "aria-labelledby": "no-credentials-list-item"
      }, React.createElement(DataListItemRow, {
        key: "no-credentials-list-item-row"
      }, React.createElement(DataListItemCells, {
        dataListCells: [React.createElement(DataListCell, {
          key: 'no-credentials-cell-0'
        }), React.createElement("strong", {
          id: `${type}-not-set-up`,
          key: 'no-credentials-cell-1'
        }, React.createElement(Msg, {
          msgKey: "notSetUp",
          params: [localizedDisplayName]
        })), React.createElement(DataListCell, {
          key: 'no-credentials-cell-2'
        })]
      })));
    }

    userCredentials.forEach(credential => {
      if (!credential.userLabel) credential.userLabel = Msg.localize(credential.type);

      if (credential.hasOwnProperty('createdDate') && credential.createdDate && credential.createdDate > 0) {
        credential.strCreatedDate = TimeUtil.format(credential.createdDate);
      }
    });
    let updateAIA;

    if (credContainer.updateAction) {
      updateAIA = new AIACommand(keycloak, credContainer.updateAction);
    }

    return React.createElement(React.Fragment, {
      key: "userCredentials"
    }, " ", userCredentials.map(credential => React.createElement(DataListItem, {
      id: `${SigningInPage.credElementId(type, credential.id, 'row')}`,
      key: 'credential-list-item-' + credential.id,
      "aria-labelledby": 'credential-list-item-' + credential.userLabel
    }, React.createElement(DataListItemRow, {
      key: 'userCredentialRow-' + credential.id
    }, React.createElement(DataListItemCells, {
      dataListCells: this.credentialRowCells(credential, type)
    }), React.createElement(CredentialAction, {
      credential: credential,
      removeable: removeable,
      updateAction: updateAIA,
      credRemover: this.handleRemove
    })))));
  }

  credentialRowCells(credential, type) {
    const credRowCells = [];
    credRowCells.push(React.createElement(DataListCell, {
      id: `${SigningInPage.credElementId(type, credential.id, 'label')}`,
      key: 'userLabel-' + credential.id
    }, credential.userLabel));

    if (credential.strCreatedDate) {
      credRowCells.push(React.createElement(DataListCell, {
        id: `${SigningInPage.credElementId(type, credential.id, 'created-at')}`,
        key: 'created-' + credential.id
      }, React.createElement("strong", null, React.createElement(Msg, {
        msgKey: "credentialCreatedAt"
      }), ": "), credential.strCreatedDate));
      credRowCells.push(React.createElement(DataListCell, {
        key: 'spacer-' + credential.id
      }));
    }

    return credRowCells;
  }

  renderCredTypeTitle(credContainer, keycloak) {
    if (!credContainer.hasOwnProperty('helptext') && !credContainer.hasOwnProperty('createAction')) return;
    let setupAction;

    if (credContainer.createAction) {
      setupAction = new AIACommand(keycloak, credContainer.createAction);
    }

    const credContainerDisplayName = Msg.localize(credContainer.displayName);
    return React.createElement(React.Fragment, {
      key: 'credTypeTitle-' + credContainer.type
    }, React.createElement(DataListItem, {
      "aria-labelledby": 'type-datalistitem-' + credContainer.type
    }, React.createElement(DataListItemRow, {
      key: 'credTitleRow-' + credContainer.type
    }, React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, {
        width: 5,
        key: 'credTypeTitle-' + credContainer.type
      }, React.createElement(Title, {
        headingLevel: TitleLevel.h3,
        size: "2xl"
      }, React.createElement("strong", {
        id: `${credContainer.type}-cred-title`
      }, React.createElement(Msg, {
        msgKey: credContainer.displayName
      }))), React.createElement("span", {
        id: `${credContainer.type}-cred-help`
      }, credContainer.helptext && React.createElement(Msg, {
        msgKey: credContainer.helptext
      })))]
    }), credContainer.createAction && React.createElement(DataListAction, {
      "aria-labelledby": "create",
      "aria-label": "create action",
      id: 'mob-setUpAction-' + credContainer.type,
      className: DataListActionVisibility.hiddenOnLg
    }, React.createElement(Dropdown, {
      isPlain: true,
      position: DropdownPosition.right,
      toggle: React.createElement(KebabToggle, {
        onToggle: isOpen => this.setState({
          toggle: isOpen
        })
      }),
      isOpen: this.state.toggle,
      dropdownItems: [React.createElement("button", {
        id: `mob-${credContainer.type}-set-up`,
        className: "pf-c-button pf-m-link",
        type: "button",
        onClick: () => setupAction.execute()
      }, React.createElement("span", {
        className: "pf-c-button__icon"
      }, React.createElement("i", {
        className: "fas fa-plus-circle",
        "aria-hidden": "true"
      })), React.createElement(Msg, {
        msgKey: "setUpNew",
        params: [credContainerDisplayName]
      }))]
    })), credContainer.createAction && React.createElement(DataListAction, {
      "aria-labelledby": "create",
      "aria-label": "create action",
      id: 'setUpAction-' + credContainer.type,
      className: css(DataListActionVisibility.visibleOnLg, DataListActionVisibility.hidden)
    }, React.createElement("button", {
      id: `${credContainer.type}-set-up`,
      className: "pf-c-button pf-m-link",
      type: "button",
      onClick: () => setupAction.execute()
    }, React.createElement("span", {
      className: "pf-c-button__icon"
    }, React.createElement("i", {
      className: "fas fa-plus-circle",
      "aria-hidden": "true"
    })), React.createElement(Msg, {
      msgKey: "setUpNew",
      params: [credContainerDisplayName]
    }))))));
  }

}

_defineProperty(SigningInPage, "contextType", AccountServiceContext);

;
;

class CredentialAction extends React.Component {
  render() {
    if (this.props.updateAction) {
      return React.createElement(DataListAction, {
        "aria-labelledby": "foo",
        "aria-label": "foo action",
        id: 'updateAction-' + this.props.credential.id
      }, React.createElement(Button, {
        id: `${SigningInPage.credElementId(this.props.credential.type, this.props.credential.id, 'update')}`,
        variant: "primary",
        onClick: () => this.props.updateAction.execute()
      }, React.createElement(Msg, {
        msgKey: "update"
      })));
    }

    if (this.props.removeable) {
      const userLabel = this.props.credential.userLabel;
      return React.createElement(DataListAction, {
        "aria-labelledby": "foo",
        "aria-label": "foo action",
        id: 'removeAction-' + this.props.credential.id
      }, React.createElement(ContinueCancelModal, {
        buttonTitle: "remove",
        buttonId: `${SigningInPage.credElementId(this.props.credential.type, this.props.credential.id, 'remove')}`,
        modalTitle: Msg.localize('removeCred', [userLabel]),
        modalMessage: Msg.localize('stopUsingCred', [userLabel]),
        onContinue: () => this.props.credRemover(this.props.credential.id, userLabel)
      }));
    }

    return React.createElement(React.Fragment, null);
  }

}

const SigningInPageWithRouter = withRouter(SigningInPage);
export { SigningInPageWithRouter as SigningInPage };
//# sourceMappingURL=SigningInPage.js.map