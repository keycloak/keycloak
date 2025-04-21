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
import { Alert, Button, DataList, DataListAction, DataListItemCells, DataListCell, DataListItem, DataListItemRow, EmptyState, EmptyStateVariant, EmptyStateBody, Split, SplitItem, Title, Dropdown, DropdownPosition, KebabToggle, PageSection, PageSectionVariants } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { AIACommand } from "../../util/AIACommand.js";
import TimeUtil from "../../util/TimeUtil.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { ContinueCancelModal } from "../../widgets/ContinueCancelModal.js";
import { Msg } from "../../widgets/Msg.js";
import { ContentPage } from "../ContentPage.js";
import { ContentAlert } from "../ContentAlert.js";
import { KeycloakContext } from "../../keycloak-service/KeycloakContext.js";

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
class SigningInPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "handleRemove", (credentialId, userLabel) => {
      this.context.doDelete("/credentials/" + encodeURIComponent(credentialId)).then(() => {
        this.getCredentialContainers();
        ContentAlert.success("successRemovedMessage", [userLabel]);
      });
    });

    this.context = context;
    this.state = {
      credentialContainers: new Map()
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
    });
  }

  static credElementId(credType, credId, item) {
    return `${credType}-${item}-${credId.substring(0, 8)}`;
  }

  render() {
    return /*#__PURE__*/React.createElement(ContentPage, {
      title: "signingIn",
      introMessage: "signingInSubMessage"
    }, this.renderCategories());
  }

  renderCategories() {
    return Array.from(this.state.credentialContainers.keys()).map(category => /*#__PURE__*/React.createElement(PageSection, {
      key: category,
      variant: PageSectionVariants.light
    }, /*#__PURE__*/React.createElement(Title, {
      id: `${category}-categ-title`,
      headingLevel: "h2",
      size: "xl"
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: category
    })), this.renderTypes(category)));
  }

  renderTypes(category) {
    let credTypeMap = this.state.credentialContainers.get(category);
    return /*#__PURE__*/React.createElement(KeycloakContext.Consumer, null, keycloak => /*#__PURE__*/React.createElement(React.Fragment, null, Array.from(credTypeMap.keys()).map((credType, index, typeArray) => [this.renderCredTypeTitle(credTypeMap.get(credType), keycloak, category), this.renderUserCredentials(credTypeMap, credType, keycloak)])));
  }

  renderEmptyRow(type, isLast) {
    if (isLast) return; // don't put empty row at the end

    return /*#__PURE__*/React.createElement(DataListItem, {
      "aria-labelledby": "empty-list-item-" + type
    }, /*#__PURE__*/React.createElement(DataListItemRow, {
      key: "empty-row-" + type
    }, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, null)]
    })));
  }

  renderUserCredentials(credTypeMap, credType, keycloak) {
    const credContainer = credTypeMap.get(credType);
    const userCredentialMetadatas = credContainer.userCredentialMetadatas;
    const removeable = credContainer.removeable;
    const type = credContainer.type;
    const displayName = credContainer.displayName;

    if (!userCredentialMetadatas || userCredentialMetadatas.length === 0) {
      const localizedDisplayName = Msg.localize(displayName);
      return /*#__PURE__*/React.createElement(DataList, {
        "aria-label": Msg.localize('notSetUp', [localizedDisplayName]),
        className: "pf-u-mb-xl"
      }, /*#__PURE__*/React.createElement(DataListItem, {
        key: "no-credentials-list-item",
        "aria-labelledby": Msg.localize('notSetUp', [localizedDisplayName])
      }, /*#__PURE__*/React.createElement(DataListItemRow, {
        key: "no-credentials-list-item-row",
        className: "pf-u-align-items-center"
      }, /*#__PURE__*/React.createElement(DataListItemCells, {
        dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
          key: 'no-credentials-cell-0'
        }), /*#__PURE__*/React.createElement(EmptyState, {
          id: `${type}-not-set-up`,
          key: 'no-credentials-cell-1',
          variant: EmptyStateVariant.xs
        }, /*#__PURE__*/React.createElement(EmptyStateBody, null, /*#__PURE__*/React.createElement(Msg, {
          msgKey: "notSetUp",
          params: [localizedDisplayName]
        }))), /*#__PURE__*/React.createElement(DataListCell, {
          key: 'no-credentials-cell-2'
        })]
      }))));
    }

    userCredentialMetadatas.forEach(credentialMetadata => {
      let credential = credentialMetadata.credential;
      if (!credential.userLabel) credential.userLabel = Msg.localize(credential.type);

      if (credential.hasOwnProperty('createdDate') && credential.createdDate && credential.createdDate > 0) {
        credential.strCreatedDate = TimeUtil.format(credential.createdDate);
      }
    });
    let updateAIA;

    if (credContainer.updateAction) {
      updateAIA = new AIACommand(keycloak, credContainer.updateAction);
    }

    let maxWidth = {
      maxWidth: 689
    };
    return /*#__PURE__*/React.createElement(React.Fragment, {
      key: "userCredentialMetadatas"
    }, " ", userCredentialMetadatas.map(credentialMetadata => /*#__PURE__*/React.createElement(React.Fragment, null, credentialMetadata.infoMessage && !credentialMetadata.warningMessageTitle && !credentialMetadata.warningMessageDescription && /*#__PURE__*/React.createElement(Alert, {
      variant: "default",
      className: "pf-u-mb-md",
      isInline: true,
      isPlain: true,
      title: Msg.localize(JSON.parse(credentialMetadata.infoMessage).key, JSON.parse(credentialMetadata.infoMessage).parameters)
    }), credentialMetadata.warningMessageTitle && credentialMetadata.warningMessageDescription && /*#__PURE__*/React.createElement(Alert, {
      variant: "warning",
      className: "pf-u-mb-md",
      isInline: true,
      title: Msg.localize(JSON.parse(credentialMetadata.warningMessageTitle).key, JSON.parse(credentialMetadata.warningMessageTitle).parameters),
      style: maxWidth
    }, /*#__PURE__*/React.createElement("p", null, Msg.localize(JSON.parse(credentialMetadata.warningMessageDescription).key, JSON.parse(credentialMetadata.warningMessageDescription).parameters))), /*#__PURE__*/React.createElement(DataList, {
      "aria-label": "user credential",
      className: "pf-u-mb-xl"
    }, /*#__PURE__*/React.createElement(DataListItem, {
      id: `${SigningInPage.credElementId(type, credentialMetadata.credential.id, 'row')}`,
      key: 'credential-list-item-' + credentialMetadata.credential.id,
      "aria-labelledby": 'credential-list-item-' + credentialMetadata.credential.userLabel
    }, /*#__PURE__*/React.createElement(DataListItemRow, {
      key: 'userCredentialRow-' + credentialMetadata.credential.id,
      className: "pf-u-align-items-center"
    }, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: this.credentialRowCells(credentialMetadata, type)
    }), /*#__PURE__*/React.createElement(CredentialAction, {
      credential: credentialMetadata.credential,
      removeable: removeable,
      updateAction: updateAIA,
      credRemover: this.handleRemove
    })))))), " ");
  }

  credentialRowCells(credMetadata, type) {
    const credRowCells = [];
    const credential = credMetadata.credential;
    let maxWidth = {
      "--pf-u-max-width--MaxWidth": "300px"
    };
    credRowCells.push( /*#__PURE__*/React.createElement(DataListCell, {
      id: `${SigningInPage.credElementId(type, credential.id, 'label')}`,
      key: 'userLabel-' + credential.id,
      className: "pf-u-max-width",
      style: maxWidth
    }, credential.userLabel));

    if (credential.strCreatedDate) {
      credRowCells.push( /*#__PURE__*/React.createElement(DataListCell, {
        id: `${SigningInPage.credElementId(type, credential.id, "created-at")}`,
        key: "created-" + credential.id
      }, /*#__PURE__*/React.createElement("strong", {
        className: "pf-u-mr-md"
      }, /*#__PURE__*/React.createElement(Msg, {
        msgKey: "credentialCreatedAt"
      }), " "), credential.strCreatedDate));
      credRowCells.push( /*#__PURE__*/React.createElement(DataListCell, {
        key: "spacer-" + credential.id
      }));
    }

    return credRowCells;
  }

  renderCredTypeTitle(credContainer, keycloak, category) {
    if (!credContainer.hasOwnProperty("helptext") && !credContainer.hasOwnProperty("createAction")) return;
    let setupAction;

    if (credContainer.createAction) {
      setupAction = new AIACommand(keycloak, credContainer.createAction);
    }

    const credContainerDisplayName = Msg.localize(credContainer.displayName);
    return /*#__PURE__*/React.createElement(React.Fragment, {
      key: "credTypeTitle-" + credContainer.type
    }, /*#__PURE__*/React.createElement(Split, {
      className: "pf-u-mt-lg pf-u-mb-lg"
    }, /*#__PURE__*/React.createElement(SplitItem, null, /*#__PURE__*/React.createElement(Title, {
      headingLevel: "h3",
      size: "md",
      className: "pf-u-mb-md"
    }, /*#__PURE__*/React.createElement("span", {
      className: "cred-title pf-u-display-block",
      id: `${credContainer.type}-cred-title`
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: credContainer.displayName
    }))), /*#__PURE__*/React.createElement("span", {
      id: `${credContainer.type}-cred-help`
    }, credContainer.helptext && /*#__PURE__*/React.createElement(Msg, {
      msgKey: credContainer.helptext
    }))), /*#__PURE__*/React.createElement(SplitItem, {
      isFilled: true
    }, credContainer.createAction && /*#__PURE__*/React.createElement("div", {
      id: "mob-setUpAction-" + credContainer.type,
      className: "pf-u-display-none-on-lg pf-u-float-right"
    }, /*#__PURE__*/React.createElement(Dropdown, {
      isPlain: true,
      position: DropdownPosition.right,
      toggle: /*#__PURE__*/React.createElement(KebabToggle, {
        onToggle: isOpen => {
          credContainer.open = isOpen;
          this.setState({
            credentialContainers: new Map(this.state.credentialContainers)
          });
        }
      }),
      isOpen: credContainer.open,
      dropdownItems: [/*#__PURE__*/React.createElement("button", {
        id: `mob-${credContainer.type}-set-up`,
        className: "pf-c-button pf-m-link",
        type: "button",
        onClick: () => setupAction.execute()
      }, /*#__PURE__*/React.createElement("span", {
        className: "pf-c-button__icon"
      }, /*#__PURE__*/React.createElement("i", {
        className: "fa fa-plus-circle",
        "aria-hidden": "true"
      })), /*#__PURE__*/React.createElement(Msg, {
        msgKey: "setUpNew",
        params: [credContainerDisplayName]
      }))]
    })), credContainer.createAction && /*#__PURE__*/React.createElement("div", {
      id: "setUpAction-" + credContainer.type,
      className: "pf-u-display-none pf-u-display-inline-flex-on-lg pf-u-float-right"
    }, /*#__PURE__*/React.createElement("button", {
      id: `${credContainer.type}-set-up`,
      className: "pf-c-button pf-m-link",
      type: "button",
      onClick: () => setupAction.execute()
    }, /*#__PURE__*/React.createElement("span", {
      className: "pf-c-button__icon"
    }, /*#__PURE__*/React.createElement("i", {
      className: "fa fa-plus-circle",
      "aria-hidden": "true"
    })), /*#__PURE__*/React.createElement(Msg, {
      msgKey: "setUpNew",
      params: [credContainerDisplayName]
    }))))));
  }

}

_defineProperty(SigningInPage, "contextType", AccountServiceContext);

;

class CredentialAction extends React.Component {
  render() {
    if (this.props.updateAction) {
      return /*#__PURE__*/React.createElement(DataListAction, {
        "aria-labelledby": Msg.localize('updateCredAriaLabel'),
        "aria-label": Msg.localize('updateCredAriaLabel'),
        id: "updateAction-" + this.props.credential.id
      }, /*#__PURE__*/React.createElement(Button, {
        variant: "secondary",
        id: `${SigningInPage.credElementId(this.props.credential.type, this.props.credential.id, "update")}`,
        onClick: () => this.props.updateAction.execute()
      }, /*#__PURE__*/React.createElement(Msg, {
        msgKey: "update"
      })));
    }

    if (this.props.removeable) {
      const userLabel = this.props.credential.userLabel;
      return /*#__PURE__*/React.createElement(DataListAction, {
        "aria-label": Msg.localize('removeCredAriaLabel'),
        "aria-labelledby": Msg.localize('removeCredAriaLabel'),
        id: 'removeAction-' + this.props.credential.id
      }, /*#__PURE__*/React.createElement(ContinueCancelModal, {
        buttonTitle: "remove",
        buttonVariant: "danger",
        buttonId: `${SigningInPage.credElementId(this.props.credential.type, this.props.credential.id, 'remove')}`,
        modalTitle: Msg.localize('removeCred', [userLabel]),
        modalMessage: Msg.localize('stopUsingCred', [userLabel]),
        onContinue: () => this.props.credRemover(this.props.credential.id, userLabel)
      }));
    }

    return /*#__PURE__*/React.createElement(React.Fragment, null);
  }

}

const SigningInPageWithRouter = withRouter(SigningInPage);
export { SigningInPageWithRouter as SigningInPage };
//# sourceMappingURL=SigningInPage.js.map