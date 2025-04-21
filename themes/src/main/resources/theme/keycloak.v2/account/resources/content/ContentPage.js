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
import * as React from "../../../common/keycloak/web_modules/react.js";
import { Button, Text, Title, Tooltip, PageSection, TextContent, PageSectionVariants, SplitItem, Split } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { SyncAltIcon } from "../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { Msg } from "../widgets/Msg.js";
import { ContentAlert } from "./ContentAlert.js";

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 */
export class ContentPage extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(ContentAlert, null), /*#__PURE__*/React.createElement(PageSection, {
      variant: PageSectionVariants.light,
      className: "pf-u-pb-xs"
    }, /*#__PURE__*/React.createElement(Split, null, /*#__PURE__*/React.createElement(SplitItem, {
      isFilled: true
    }, /*#__PURE__*/React.createElement(TextContent, null, /*#__PURE__*/React.createElement(Title, {
      headingLevel: "h1",
      size: "2xl",
      className: "pf-u-mb-xl"
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: this.props.title
    })), this.props.introMessage && /*#__PURE__*/React.createElement(Text, {
      component: "p"
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: this.props.introMessage
    })))), this.props.onRefresh && /*#__PURE__*/React.createElement(SplitItem, null, /*#__PURE__*/React.createElement(Tooltip, {
      content: /*#__PURE__*/React.createElement(Msg, {
        msgKey: "refreshPage"
      })
    }, /*#__PURE__*/React.createElement(Button, {
      "aria-label": Msg.localize('refreshPage'),
      id: "refresh-page",
      variant: "link",
      onClick: this.props.onRefresh,
      icon: /*#__PURE__*/React.createElement(SyncAltIcon, null)
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "refresh"
    })))))), this.props.children);
  }

}
;
//# sourceMappingURL=ContentPage.js.map