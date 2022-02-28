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
import { Button, Grid, GridItem, Title, Tooltip } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { RedoIcon } from "../../../common/keycloak/web_modules/@patternfly/react-icons.js";
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
    return React.createElement(React.Fragment, null, React.createElement(ContentAlert, null), React.createElement("section", {
      id: "page-heading",
      className: "pf-c-page__main-section pf-m-light"
    }, React.createElement(Grid, null, React.createElement(GridItem, {
      span: 11
    }, React.createElement(Title, {
      headingLevel: "h1",
      size: "3xl"
    }, React.createElement("strong", null, React.createElement(Msg, {
      msgKey: this.props.title
    })))), this.props.onRefresh && React.createElement(GridItem, {
      span: 1
    }, React.createElement(Tooltip, {
      content: React.createElement(Msg, {
        msgKey: "refreshPage"
      })
    }, React.createElement(Button, {
      "aria-describedby": "refresh page",
      id: "refresh-page",
      variant: "plain",
      onClick: this.props.onRefresh
    }, React.createElement(RedoIcon, {
      size: "sm"
    })))), this.props.introMessage && React.createElement(GridItem, {
      span: 12
    }, " ", React.createElement(Msg, {
      msgKey: this.props.introMessage
    })))), React.createElement("section", {
      className: "pf-c-page__main-section pf-m-no-padding-mobile"
    }, this.props.children));
  }

}
;
//# sourceMappingURL=ContentPage.js.map