function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

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
import * as React from "../../../common/keycloak/web_modules/react.js";
import { Modal, Button } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { Msg } from "./Msg.js";
/**
 * For any of these properties that are strings, you can
 * pass in a localization key instead of a static string.
 */

/**
 * This class renders a button that provides a continue/cancel modal dialog when clicked.  If the user selects 'Continue'
 * then the onContinue function is executed.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 */
export class ContinueCancelModal extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleModalToggle", () => {
      this.setState(({
        isModalOpen
      }) => ({
        isModalOpen: !isModalOpen
      }));
      if (this.props.onClose) this.props.onClose();
    });

    _defineProperty(this, "handleContinue", () => {
      this.handleModalToggle();
      this.props.onContinue();
    });

    this.state = {
      isModalOpen: false
    };
  }

  render() {
    const {
      isModalOpen
    } = this.state;
    return React.createElement(React.Fragment, null, !this.props.render && React.createElement(Button, {
      id: this.props.buttonId,
      variant: this.props.buttonVariant,
      onClick: this.handleModalToggle,
      isDisabled: this.props.isDisabled
    }, React.createElement(Msg, {
      msgKey: this.props.buttonTitle
    })), this.props.render && this.props.render(this.handleModalToggle), React.createElement(Modal, _extends({}, this.props, {
      title: Msg.localize(this.props.modalTitle),
      isOpen: isModalOpen,
      onClose: this.handleModalToggle,
      actions: [React.createElement(Button, {
        id: "modal-cancel",
        key: "cancel",
        variant: "secondary",
        onClick: this.handleModalToggle
      }, React.createElement(Msg, {
        msgKey: this.props.modalCancelButtonLabel
      })), React.createElement(Button, {
        id: "modal-confirm",
        key: "confirm",
        variant: "primary",
        onClick: this.handleContinue
      }, React.createElement(Msg, {
        msgKey: this.props.modalContinueButtonLabel
      }))]
    }), !this.props.modalMessage && this.props.children, this.props.modalMessage && React.createElement(Msg, {
      msgKey: this.props.modalMessage
    })));
  }

}

_defineProperty(ContinueCancelModal, "defaultProps", {
  buttonVariant: 'primary',
  modalContinueButtonLabel: 'continue',
  modalCancelButtonLabel: 'doCancel',
  isDisabled: false,
  isSmall: true
});

;
//# sourceMappingURL=ContinueCancelModal.js.map