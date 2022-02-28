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
import * as React from "../../../common/keycloak/web_modules/react.js";
import { EmptyState, EmptyStateVariant, Title, EmptyStateIcon, TitleLevel, EmptyStateBody } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { Msg } from "./Msg.js";
export default class EmptyMessageState extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return React.createElement(EmptyState, {
      variant: EmptyStateVariant.full
    }, React.createElement(EmptyStateIcon, {
      icon: this.props.icon
    }), React.createElement(Title, {
      headingLevel: TitleLevel.h5,
      size: "lg"
    }, React.createElement(Msg, {
      msgKey: this.props.messageKey
    })), React.createElement(EmptyStateBody, null, this.props.children));
  }

}
//# sourceMappingURL=EmptyMessageState.js.map