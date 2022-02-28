"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.LoginPage = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _BackgroundImage = require("../BackgroundImage");

var _Brand = require("../Brand");

var _List = require("../List");

var _Login = require("./Login");

var _LoginHeader = require("./LoginHeader");

var _LoginFooter = require("./LoginFooter");

var _LoginMainHeader = require("./LoginMainHeader");

var _LoginMainBody = require("./LoginMainBody");

var _LoginMainFooter = require("./LoginMainFooter");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var LoginPage = function LoginPage(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$brandImgSrc = _ref.brandImgSrc,
      brandImgSrc = _ref$brandImgSrc === void 0 ? '' : _ref$brandImgSrc,
      _ref$brandImgAlt = _ref.brandImgAlt,
      brandImgAlt = _ref$brandImgAlt === void 0 ? '' : _ref$brandImgAlt,
      _ref$backgroundImgSrc = _ref.backgroundImgSrc,
      backgroundImgSrc = _ref$backgroundImgSrc === void 0 ? '' : _ref$backgroundImgSrc,
      _ref$backgroundImgAlt = _ref.backgroundImgAlt,
      backgroundImgAlt = _ref$backgroundImgAlt === void 0 ? '' : _ref$backgroundImgAlt,
      _ref$footerListItems = _ref.footerListItems,
      footerListItems = _ref$footerListItems === void 0 ? null : _ref$footerListItems,
      _ref$textContent = _ref.textContent,
      textContent = _ref$textContent === void 0 ? '' : _ref$textContent,
      footerListVariants = _ref.footerListVariants,
      loginTitle = _ref.loginTitle,
      loginSubtitle = _ref.loginSubtitle,
      _ref$signUpForAccount = _ref.signUpForAccountMessage,
      signUpForAccountMessage = _ref$signUpForAccount === void 0 ? null : _ref$signUpForAccount,
      _ref$forgotCredential = _ref.forgotCredentials,
      forgotCredentials = _ref$forgotCredential === void 0 ? null : _ref$forgotCredential,
      _ref$socialMediaLogin = _ref.socialMediaLoginContent,
      socialMediaLoginContent = _ref$socialMediaLogin === void 0 ? null : _ref$socialMediaLogin,
      props = _objectWithoutProperties(_ref, ["children", "className", "brandImgSrc", "brandImgAlt", "backgroundImgSrc", "backgroundImgAlt", "footerListItems", "textContent", "footerListVariants", "loginTitle", "loginSubtitle", "signUpForAccountMessage", "forgotCredentials", "socialMediaLoginContent"]);

  var HeaderBrand = React.createElement(React.Fragment, null, React.createElement(_Brand.Brand, {
    src: brandImgSrc,
    alt: brandImgAlt
  }));
  var Header = React.createElement(_LoginHeader.LoginHeader, {
    headerBrand: HeaderBrand
  });
  var Footer = React.createElement(_LoginFooter.LoginFooter, null, React.createElement("p", null, textContent), React.createElement(_List.List, {
    variant: footerListVariants
  }, footerListItems));
  return React.createElement(React.Fragment, null, backgroundImgSrc && React.createElement(_BackgroundImage.BackgroundImage, {
    src: backgroundImgSrc,
    alt: backgroundImgAlt
  }), React.createElement(_Login.Login, _extends({
    header: Header,
    footer: Footer,
    className: (0, _reactStyles.css)(className)
  }, props), React.createElement(_LoginMainHeader.LoginMainHeader, {
    title: loginTitle,
    subtitle: loginSubtitle
  }), React.createElement(_LoginMainBody.LoginMainBody, null, children), (socialMediaLoginContent || forgotCredentials || signUpForAccountMessage) && React.createElement(_LoginMainFooter.LoginMainFooter, {
    socialMediaLoginContent: socialMediaLoginContent,
    forgotCredentials: forgotCredentials,
    signUpForAccountMessage: signUpForAccountMessage
  })));
};

exports.LoginPage = LoginPage;
LoginPage.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  brandImgSrc: _propTypes["default"].string,
  brandImgAlt: _propTypes["default"].string,
  backgroundImgSrc: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].any]),
  backgroundImgAlt: _propTypes["default"].string,
  textContent: _propTypes["default"].string,
  footerListItems: _propTypes["default"].node,
  footerListVariants: _propTypes["default"].any,
  loginTitle: _propTypes["default"].string.isRequired,
  loginSubtitle: _propTypes["default"].string,
  signUpForAccountMessage: _propTypes["default"].node,
  forgotCredentials: _propTypes["default"].node,
  socialMediaLoginContent: _propTypes["default"].node
};
//# sourceMappingURL=LoginPage.js.map