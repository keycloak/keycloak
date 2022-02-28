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
export class Msg extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    if (this.props.children) {
      return Msg.localizeWithChildren(this.props.msgKey, this.props.children);
    }

    return React.createElement(React.Fragment, null, Msg.localize(this.props.msgKey, this.props.params));
  }

  static localizeWithChildren(msgKey, children) {
    const message = l18nMsg[this.processKey(msgKey)];
    const parts = message.split(/\{\{param_\d*}}/);
    const count = React.Children.count(children);
    return React.Children.map(children, (child, i) => [parts[i], child, count === i + 1 ? parts[count] : '']);
  }

  static localize(msgKey, params) {
    let message = l18nMsg[this.processKey(msgKey)];
    if (message === undefined) message = msgKey;

    if (params !== undefined && params.length > 0) {
      params.forEach((value, index) => {
        value = this.processParam(value);
        message = message.replace('{{param_' + index + '}}', value);
      });
    }

    return unescape(message);
  } // if the message key has Freemarker syntax, remove it


  static processKey(msgKey) {
    if (!(msgKey.startsWith('${') && msgKey.endsWith('}'))) return msgKey; // remove Freemarker syntax

    return msgKey.substring(2, msgKey.length - 1);
  } // if the param has Freemarker syntax, try to look up its value


  static processParam(param) {
    if (!(param.startsWith('${') && param.endsWith('}'))) return param; // remove Freemarker syntax

    const key = param.substring(2, param.length - 1);
    let value = l18nMsg[key];
    if (value === undefined) return param;
    return value;
  }

}
//# sourceMappingURL=Msg.js.map