"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.LoginForm = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _Form = require("../Form");

var _TextInput = require("../TextInput");

var _Button = require("../Button");

var _Checkbox = require("../Checkbox");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var LoginForm = function LoginForm(_ref) {
  var _ref$noAutoFocus = _ref.noAutoFocus,
      noAutoFocus = _ref$noAutoFocus === void 0 ? false : _ref$noAutoFocus,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$showHelperText = _ref.showHelperText,
      showHelperText = _ref$showHelperText === void 0 ? false : _ref$showHelperText,
      _ref$helperText = _ref.helperText,
      helperText = _ref$helperText === void 0 ? null : _ref$helperText,
      _ref$usernameLabel = _ref.usernameLabel,
      usernameLabel = _ref$usernameLabel === void 0 ? 'Username' : _ref$usernameLabel,
      _ref$usernameValue = _ref.usernameValue,
      usernameValue = _ref$usernameValue === void 0 ? '' : _ref$usernameValue,
      _ref$onChangeUsername = _ref.onChangeUsername,
      onChangeUsername = _ref$onChangeUsername === void 0 ? function () {
    return undefined;
  } : _ref$onChangeUsername,
      _ref$isValidUsername = _ref.isValidUsername,
      isValidUsername = _ref$isValidUsername === void 0 ? true : _ref$isValidUsername,
      _ref$passwordLabel = _ref.passwordLabel,
      passwordLabel = _ref$passwordLabel === void 0 ? 'Password' : _ref$passwordLabel,
      _ref$passwordValue = _ref.passwordValue,
      passwordValue = _ref$passwordValue === void 0 ? '' : _ref$passwordValue,
      _ref$onChangePassword = _ref.onChangePassword,
      onChangePassword = _ref$onChangePassword === void 0 ? function () {
    return undefined;
  } : _ref$onChangePassword,
      _ref$isValidPassword = _ref.isValidPassword,
      isValidPassword = _ref$isValidPassword === void 0 ? true : _ref$isValidPassword,
      _ref$loginButtonLabel = _ref.loginButtonLabel,
      loginButtonLabel = _ref$loginButtonLabel === void 0 ? 'Log In' : _ref$loginButtonLabel,
      _ref$isLoginButtonDis = _ref.isLoginButtonDisabled,
      isLoginButtonDisabled = _ref$isLoginButtonDis === void 0 ? false : _ref$isLoginButtonDis,
      _ref$onLoginButtonCli = _ref.onLoginButtonClick,
      onLoginButtonClick = _ref$onLoginButtonCli === void 0 ? function () {
    return undefined;
  } : _ref$onLoginButtonCli,
      _ref$rememberMeLabel = _ref.rememberMeLabel,
      rememberMeLabel = _ref$rememberMeLabel === void 0 ? '' : _ref$rememberMeLabel,
      _ref$isRememberMeChec = _ref.isRememberMeChecked,
      isRememberMeChecked = _ref$isRememberMeChec === void 0 ? false : _ref$isRememberMeChec,
      _ref$onChangeRemember = _ref.onChangeRememberMe,
      onChangeRememberMe = _ref$onChangeRemember === void 0 ? function () {
    return undefined;
  } : _ref$onChangeRemember,
      _ref$rememberMeAriaLa = _ref.rememberMeAriaLabel,
      rememberMeAriaLabel = _ref$rememberMeAriaLa === void 0 ? '' : _ref$rememberMeAriaLa,
      props = _objectWithoutProperties(_ref, ["noAutoFocus", "className", "showHelperText", "helperText", "usernameLabel", "usernameValue", "onChangeUsername", "isValidUsername", "passwordLabel", "passwordValue", "onChangePassword", "isValidPassword", "loginButtonLabel", "isLoginButtonDisabled", "onLoginButtonClick", "rememberMeLabel", "isRememberMeChecked", "onChangeRememberMe", "rememberMeAriaLabel"]);

  return React.createElement(_Form.Form, _extends({
    className: className
  }, props), React.createElement(_Form.FormHelperText, {
    isError: !isValidUsername || !isValidPassword,
    isHidden: !showHelperText
  }, helperText), React.createElement(_Form.FormGroup, {
    label: usernameLabel,
    isRequired: true,
    isValid: isValidUsername,
    fieldId: "pf-login-username-id"
  }, React.createElement(_TextInput.TextInput, {
    autoFocus: !noAutoFocus,
    id: "pf-login-username-id",
    isRequired: true,
    isValid: isValidUsername,
    type: "text",
    name: "pf-login-username-id",
    value: usernameValue,
    onChange: onChangeUsername
  })), React.createElement(_Form.FormGroup, {
    label: passwordLabel,
    isRequired: true,
    isValid: isValidPassword,
    fieldId: "pf-login-password-id"
  }, React.createElement(_TextInput.TextInput, {
    isRequired: true,
    type: "password",
    id: "pf-login-password-id",
    name: "pf-login-password-id",
    isValid: isValidPassword,
    value: passwordValue,
    onChange: onChangePassword
  })), rememberMeLabel.length > 0 && React.createElement(_Form.FormGroup, {
    fieldId: "pf-login-remember-me-id"
  }, React.createElement(_Checkbox.Checkbox, {
    id: "pf-login-remember-me-id",
    label: rememberMeLabel,
    isChecked: isRememberMeChecked,
    onChange: onChangeRememberMe
  })), React.createElement(_Form.ActionGroup, null, React.createElement(_Button.Button, {
    variant: "primary",
    type: "submit",
    onClick: onLoginButtonClick,
    isBlock: true,
    isDisabled: isLoginButtonDisabled
  }, loginButtonLabel)));
};

exports.LoginForm = LoginForm;
LoginForm.propTypes = {
  noAutoFocus: _propTypes["default"].bool,
  className: _propTypes["default"].string,
  showHelperText: _propTypes["default"].bool,
  helperText: _propTypes["default"].node,
  usernameLabel: _propTypes["default"].string,
  usernameValue: _propTypes["default"].string,
  onChangeUsername: _propTypes["default"].func,
  isValidUsername: _propTypes["default"].bool,
  passwordLabel: _propTypes["default"].string,
  passwordValue: _propTypes["default"].string,
  onChangePassword: _propTypes["default"].func,
  isValidPassword: _propTypes["default"].bool,
  loginButtonLabel: _propTypes["default"].string,
  isLoginButtonDisabled: _propTypes["default"].bool,
  onLoginButtonClick: _propTypes["default"].func,
  rememberMeLabel: _propTypes["default"].string,
  isRememberMeChecked: _propTypes["default"].bool,
  onChangeRememberMe: _propTypes["default"].func,
  rememberMeAriaLabel: _propTypes["default"].string
};
//# sourceMappingURL=LoginForm.js.map