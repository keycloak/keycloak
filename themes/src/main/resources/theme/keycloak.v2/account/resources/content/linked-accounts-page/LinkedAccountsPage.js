function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
import { Button, DataList, DataListAction, DataListItemCells, DataListCell, DataListItemRow, Label, PageSection, PageSectionVariants, Split, SplitItem, Stack, StackItem, Title, DataListItem } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { BitbucketIcon, CubeIcon, GitlabIcon, LinkIcon, OpenshiftIcon, PaypalIcon, UnlinkIcon, FacebookIcon, GoogleIcon, InstagramIcon, MicrosoftIcon, TwitterIcon, StackOverflowIcon, LinkedinIcon, GithubIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { Msg } from "../../widgets/Msg.js";
import { ContentPage } from "../ContentPage.js";
import { createRedirect } from "../../util/RedirectUri.js";

/**
 * @author Stan Silvert
 */
class LinkedAccountsPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    this.context = context;
    this.state = {
      linkedAccounts: [],
      unLinkedAccounts: []
    };
    this.getLinkedAccounts();
  }

  getLinkedAccounts() {
    this.context.doGet("/linked-accounts").then(response => {
      console.log({
        response
      });
      const linkedAccounts = response.data.filter(account => account.connected);
      const unLinkedAccounts = response.data.filter(account => !account.connected);
      this.setState({
        linkedAccounts: linkedAccounts,
        unLinkedAccounts: unLinkedAccounts
      });
    });
  }

  unLinkAccount(account) {
    const url = '/linked-accounts/' + account.providerName;
    this.context.doDelete(url).then(response => {
      console.log({
        response
      });
      this.getLinkedAccounts();
    });
  }

  linkAccount(account) {
    const url = '/linked-accounts/' + account.providerName;
    const redirectUri = createRedirect(this.props.location.pathname);
    this.context.doGet(url, {
      params: {
        providerId: account.providerName,
        redirectUri
      }
    }).then(response => {
      console.log({
        response
      });
      window.location.href = response.data.accountLinkUri;
    });
  }

  render() {
    return /*#__PURE__*/React.createElement(ContentPage, {
      title: Msg.localize('linkedAccountsTitle'),
      introMessage: Msg.localize('linkedAccountsIntroMessage')
    }, /*#__PURE__*/React.createElement(PageSection, {
      isFilled: true,
      variant: PageSectionVariants.light
    }, /*#__PURE__*/React.createElement(Stack, {
      hasGutter: true
    }, /*#__PURE__*/React.createElement(StackItem, null, /*#__PURE__*/React.createElement(Title, {
      headingLevel: "h2",
      className: "pf-u-mb-lg",
      size: "xl"
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "linkedLoginProviders"
    })), /*#__PURE__*/React.createElement(DataList, {
      id: "linked-idps",
      "aria-label": Msg.localize('linkedLoginProviders')
    }, this.makeRows(this.state.linkedAccounts, true))), /*#__PURE__*/React.createElement(StackItem, null, /*#__PURE__*/React.createElement(Title, {
      headingLevel: "h2",
      className: "pf-u-mt-xl pf-u-mb-lg",
      size: "xl"
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "unlinkedLoginProviders"
    })), /*#__PURE__*/React.createElement(DataList, {
      id: "unlinked-idps",
      "aria-label": Msg.localize('unlinkedLoginProviders')
    }, this.makeRows(this.state.unLinkedAccounts, false))))));
  }

  emptyRow(isLinked) {
    let isEmptyMessage = '';

    if (isLinked) {
      isEmptyMessage = Msg.localize('linkedEmpty');
    } else {
      isEmptyMessage = Msg.localize('unlinkedEmpty');
    }

    return /*#__PURE__*/React.createElement(DataListItem, {
      key: "emptyItem",
      "aria-labelledby": Msg.localize('isEmptyMessage')
    }, /*#__PURE__*/React.createElement(DataListItemRow, {
      key: "emptyRow"
    }, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
        key: "empty"
      }, isEmptyMessage)]
    })));
  }

  makeRows(accounts, isLinked) {
    if (accounts.length === 0) {
      return this.emptyRow(isLinked);
    }

    return /*#__PURE__*/React.createElement(React.Fragment, null, " ", accounts.map(account => /*#__PURE__*/React.createElement(DataListItem, {
      id: `${account.providerAlias}-idp`,
      key: account.providerName,
      "aria-labelledby": Msg.localize('linkedAccountsTitle')
    }, /*#__PURE__*/React.createElement(DataListItemRow, {
      key: account.providerName
    }, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
        key: "idp"
      }, /*#__PURE__*/React.createElement(Split, null, /*#__PURE__*/React.createElement(SplitItem, {
        className: "pf-u-mr-sm"
      }, this.findIcon(account)), /*#__PURE__*/React.createElement(SplitItem, {
        className: "pf-u-my-xs",
        isFilled: true
      }, /*#__PURE__*/React.createElement("span", {
        id: `${account.providerAlias}-idp-name`
      }, account.displayName)))), /*#__PURE__*/React.createElement(DataListCell, {
        key: "label"
      }, /*#__PURE__*/React.createElement(Split, null, /*#__PURE__*/React.createElement(SplitItem, {
        className: "pf-u-my-xs",
        isFilled: true
      }, /*#__PURE__*/React.createElement("span", {
        id: `${account.providerAlias}-idp-label`
      }, this.label(account))))), /*#__PURE__*/React.createElement(DataListCell, {
        key: "username",
        width: 5
      }, /*#__PURE__*/React.createElement(Split, null, /*#__PURE__*/React.createElement(SplitItem, {
        className: "pf-u-my-xs",
        isFilled: true
      }, /*#__PURE__*/React.createElement("span", {
        id: `${account.providerAlias}-idp-username`
      }, account.linkedUsername))))]
    }), /*#__PURE__*/React.createElement(DataListAction, {
      "aria-labelledby": Msg.localize('link'),
      "aria-label": Msg.localize('unLink'),
      id: "setPasswordAction"
    }, isLinked && /*#__PURE__*/React.createElement(Button, {
      id: `${account.providerAlias}-idp-unlink`,
      variant: "link",
      onClick: () => this.unLinkAccount(account)
    }, /*#__PURE__*/React.createElement(UnlinkIcon, {
      size: "sm"
    }), " ", /*#__PURE__*/React.createElement(Msg, {
      msgKey: "unLink"
    })), !isLinked && /*#__PURE__*/React.createElement(Button, {
      id: `${account.providerAlias}-idp-link`,
      variant: "link",
      onClick: () => this.linkAccount(account)
    }, /*#__PURE__*/React.createElement(LinkIcon, {
      size: "sm"
    }), " ", /*#__PURE__*/React.createElement(Msg, {
      msgKey: "link"
    })))))), " ");
  }

  label(account) {
    if (account.social) {
      return /*#__PURE__*/React.createElement(Label, {
        color: "blue"
      }, /*#__PURE__*/React.createElement(Msg, {
        msgKey: "socialLogin"
      }));
    }

    return /*#__PURE__*/React.createElement(Label, {
      color: "green"
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "systemDefined"
    }));
  }

  findIcon(account) {
    const socialIconId = `${account.providerAlias}-idp-icon-social`;
    console.log(account);

    switch (true) {
      case account.providerName.toLowerCase().includes('linkedin'):
        return /*#__PURE__*/React.createElement(LinkedinIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('facebook'):
        return /*#__PURE__*/React.createElement(FacebookIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('google'):
        return /*#__PURE__*/React.createElement(GoogleIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('instagram'):
        return /*#__PURE__*/React.createElement(InstagramIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('microsoft'):
        return /*#__PURE__*/React.createElement(MicrosoftIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('bitbucket'):
        return /*#__PURE__*/React.createElement(BitbucketIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('twitter'):
        return /*#__PURE__*/React.createElement(TwitterIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('openshift'):
        // return <div className="idp-icon-social" id="openshift-idp-icon-social" />;
        return /*#__PURE__*/React.createElement(OpenshiftIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('gitlab'):
        return /*#__PURE__*/React.createElement(GitlabIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('github'):
        return /*#__PURE__*/React.createElement(GithubIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('paypal'):
        return /*#__PURE__*/React.createElement(PaypalIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes('stackoverflow'):
        return /*#__PURE__*/React.createElement(StackOverflowIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName !== '' && account.social:
        return /*#__PURE__*/React.createElement("div", {
          className: "idp-icon-social",
          id: socialIconId
        });

      default:
        return /*#__PURE__*/React.createElement(CubeIcon, {
          id: `${account.providerAlias}-idp-icon-default`,
          size: "lg"
        });
    }
  }

}

_defineProperty(LinkedAccountsPage, "contextType", AccountServiceContext);

;
const LinkedAccountsPagewithRouter = withRouter(LinkedAccountsPage);
export { LinkedAccountsPagewithRouter as LinkedAccountsPage };
//# sourceMappingURL=LinkedAccountsPage.js.map