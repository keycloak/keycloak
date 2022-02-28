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
import { Badge, Button, DataList, DataListAction, DataListItemCells, DataListCell, DataListItemRow, Stack, StackItem, Title, TitleLevel, DataListItem } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { BitbucketIcon, CubeIcon, FacebookIcon, GithubIcon, GitlabIcon, GoogleIcon, InstagramIcon, LinkIcon, LinkedinIcon, MicrosoftIcon, OpenshiftIcon, PaypalIcon, StackOverflowIcon, TwitterIcon, UnlinkIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
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
    return React.createElement(ContentPage, {
      title: Msg.localize('linkedAccountsTitle'),
      introMessage: Msg.localize('linkedAccountsIntroMessage')
    }, React.createElement(Stack, {
      gutter: "md"
    }, React.createElement(StackItem, {
      isFilled: true
    }, React.createElement(Title, {
      headingLevel: TitleLevel.h2,
      size: "2xl"
    }, React.createElement(Msg, {
      msgKey: "linkedLoginProviders"
    })), React.createElement(DataList, {
      id: "linked-idps",
      "aria-label": "foo"
    }, this.makeRows(this.state.linkedAccounts, true))), React.createElement(StackItem, {
      isFilled: true
    }), React.createElement(StackItem, {
      isFilled: true
    }, React.createElement(Title, {
      headingLevel: TitleLevel.h2,
      size: "2xl"
    }, React.createElement(Msg, {
      msgKey: "unlinkedLoginProviders"
    })), React.createElement(DataList, {
      id: "unlinked-idps",
      "aria-label": "foo"
    }, this.makeRows(this.state.unLinkedAccounts, false)))));
  }

  emptyRow(isLinked) {
    let isEmptyMessage = '';

    if (isLinked) {
      isEmptyMessage = Msg.localize('linkedEmpty');
    } else {
      isEmptyMessage = Msg.localize('unlinkedEmpty');
    }

    return React.createElement(DataListItem, {
      key: "emptyItem",
      "aria-labelledby": "empty-item"
    }, React.createElement(DataListItemRow, {
      key: "emptyRow"
    }, React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, {
        key: "empty"
      }, React.createElement("strong", null, isEmptyMessage))]
    })));
  }

  makeRows(accounts, isLinked) {
    if (accounts.length === 0) {
      return this.emptyRow(isLinked);
    }

    return React.createElement(React.Fragment, null, " ", accounts.map(account => React.createElement(DataListItem, {
      id: `${account.providerAlias}-idp`,
      key: account.providerName,
      "aria-labelledby": "simple-item1"
    }, React.createElement(DataListItemRow, {
      key: account.providerName
    }, React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, {
        key: "idp"
      }, React.createElement(Stack, null, React.createElement(StackItem, {
        isFilled: true
      }, this.findIcon(account)), React.createElement(StackItem, {
        id: `${account.providerAlias}-idp-name`,
        isFilled: true
      }, React.createElement("h2", null, React.createElement("strong", null, account.displayName))))), React.createElement(DataListCell, {
        key: "badge"
      }, React.createElement(Stack, null, React.createElement(StackItem, {
        isFilled: true
      }), React.createElement(StackItem, {
        id: `${account.providerAlias}-idp-badge`,
        isFilled: true
      }, this.badge(account)))), React.createElement(DataListCell, {
        key: "username"
      }, React.createElement(Stack, null, React.createElement(StackItem, {
        isFilled: true
      }), React.createElement(StackItem, {
        id: `${account.providerAlias}-idp-username`,
        isFilled: true
      }, account.linkedUsername)))]
    }), React.createElement(DataListAction, {
      "aria-labelledby": "foo",
      "aria-label": "foo action",
      id: "setPasswordAction"
    }, isLinked && React.createElement(Button, {
      id: `${account.providerAlias}-idp-unlink`,
      variant: "link",
      onClick: () => this.unLinkAccount(account)
    }, React.createElement(UnlinkIcon, {
      size: "sm"
    }), " ", React.createElement(Msg, {
      msgKey: "unLink"
    })), !isLinked && React.createElement(Button, {
      id: `${account.providerAlias}-idp-link`,
      variant: "link",
      onClick: () => this.linkAccount(account)
    }, React.createElement(LinkIcon, {
      size: "sm"
    }), " ", React.createElement(Msg, {
      msgKey: "link"
    })))))), " ");
  }

  badge(account) {
    if (account.social) {
      return React.createElement(Badge, null, React.createElement(Msg, {
        msgKey: "socialLogin"
      }));
    }

    return React.createElement(Badge, {
      style: {
        backgroundColor: "green"
      }
    }, React.createElement(Msg, {
      msgKey: "systemDefined"
    }));
  }

  findIcon(account) {
    const socialIconId = `${account.providerAlias}-idp-icon-social`;
    if (account.providerName.toLowerCase().includes('github')) return React.createElement(GithubIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('linkedin')) return React.createElement(LinkedinIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('facebook')) return React.createElement(FacebookIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('google')) return React.createElement(GoogleIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('instagram')) return React.createElement(InstagramIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('microsoft')) return React.createElement(MicrosoftIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('bitbucket')) return React.createElement(BitbucketIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('twitter')) return React.createElement(TwitterIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('openshift')) return React.createElement(OpenshiftIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('gitlab')) return React.createElement(GitlabIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('paypal')) return React.createElement(PaypalIcon, {
      id: socialIconId,
      size: "xl"
    });
    if (account.providerName.toLowerCase().includes('stackoverflow')) return React.createElement(StackOverflowIcon, {
      id: socialIconId,
      size: "xl"
    });
    return React.createElement(CubeIcon, {
      id: `${account.providerAlias}-idp-icon-default`,
      size: "xl"
    });
  }

}

_defineProperty(LinkedAccountsPage, "contextType", AccountServiceContext);

;
const LinkedAccountsPagewithRouter = withRouter(LinkedAccountsPage);
export { LinkedAccountsPagewithRouter as LinkedAccountsPage };
//# sourceMappingURL=LinkedAccountsPage.js.map