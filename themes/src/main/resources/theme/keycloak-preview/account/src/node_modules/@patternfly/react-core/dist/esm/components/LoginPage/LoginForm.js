import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { Form, FormGroup, ActionGroup, FormHelperText } from '../Form';
import { TextInput } from '../TextInput';
import { Button } from '../Button';
import { Checkbox } from '../Checkbox';
export const LoginForm = (_ref) => {
  let {
    noAutoFocus = false,
    className = '',
    showHelperText = false,
    helperText = null,
    usernameLabel = 'Username',
    usernameValue = '',
    onChangeUsername = () => undefined,
    isValidUsername = true,
    passwordLabel = 'Password',
    passwordValue = '',
    onChangePassword = () => undefined,
    isValidPassword = true,
    loginButtonLabel = 'Log In',
    isLoginButtonDisabled = false,
    onLoginButtonClick = () => undefined,
    rememberMeLabel = '',
    isRememberMeChecked = false,
    onChangeRememberMe = () => undefined,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    rememberMeAriaLabel = ''
  } = _ref,
      props = _objectWithoutProperties(_ref, ["noAutoFocus", "className", "showHelperText", "helperText", "usernameLabel", "usernameValue", "onChangeUsername", "isValidUsername", "passwordLabel", "passwordValue", "onChangePassword", "isValidPassword", "loginButtonLabel", "isLoginButtonDisabled", "onLoginButtonClick", "rememberMeLabel", "isRememberMeChecked", "onChangeRememberMe", "rememberMeAriaLabel"]);

  return React.createElement(Form, _extends({
    className: className
  }, props), React.createElement(FormHelperText, {
    isError: !isValidUsername || !isValidPassword,
    isHidden: !showHelperText
  }, helperText), React.createElement(FormGroup, {
    label: usernameLabel,
    isRequired: true,
    isValid: isValidUsername,
    fieldId: "pf-login-username-id"
  }, React.createElement(TextInput, {
    autoFocus: !noAutoFocus,
    id: "pf-login-username-id",
    isRequired: true,
    isValid: isValidUsername,
    type: "text",
    name: "pf-login-username-id",
    value: usernameValue,
    onChange: onChangeUsername
  })), React.createElement(FormGroup, {
    label: passwordLabel,
    isRequired: true,
    isValid: isValidPassword,
    fieldId: "pf-login-password-id"
  }, React.createElement(TextInput, {
    isRequired: true,
    type: "password",
    id: "pf-login-password-id",
    name: "pf-login-password-id",
    isValid: isValidPassword,
    value: passwordValue,
    onChange: onChangePassword
  })), rememberMeLabel.length > 0 && React.createElement(FormGroup, {
    fieldId: "pf-login-remember-me-id"
  }, React.createElement(Checkbox, {
    id: "pf-login-remember-me-id",
    label: rememberMeLabel,
    isChecked: isRememberMeChecked,
    onChange: onChangeRememberMe
  })), React.createElement(ActionGroup, null, React.createElement(Button, {
    variant: "primary",
    type: "submit",
    onClick: onLoginButtonClick,
    isBlock: true,
    isDisabled: isLoginButtonDisabled
  }, loginButtonLabel)));
};
LoginForm.propTypes = {
  noAutoFocus: _pt.bool,
  className: _pt.string,
  showHelperText: _pt.bool,
  helperText: _pt.node,
  usernameLabel: _pt.string,
  usernameValue: _pt.string,
  onChangeUsername: _pt.func,
  isValidUsername: _pt.bool,
  passwordLabel: _pt.string,
  passwordValue: _pt.string,
  onChangePassword: _pt.func,
  isValidPassword: _pt.bool,
  loginButtonLabel: _pt.string,
  isLoginButtonDisabled: _pt.bool,
  onLoginButtonClick: _pt.func,
  rememberMeLabel: _pt.string,
  isRememberMeChecked: _pt.bool,
  onChangeRememberMe: _pt.func,
  rememberMeAriaLabel: _pt.string
};
//# sourceMappingURL=LoginForm.js.map