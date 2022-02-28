import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { BackgroundImage } from '../BackgroundImage';
import { Brand } from '../Brand';
import { List } from '../List';
import { Login } from './Login';
import { LoginHeader } from './LoginHeader';
import { LoginFooter } from './LoginFooter';
import { LoginMainHeader } from './LoginMainHeader';
import { LoginMainBody } from './LoginMainBody';
import { LoginMainFooter } from './LoginMainFooter';
export const LoginPage = (_ref) => {
  let {
    children = null,
    className = '',
    brandImgSrc = '',
    brandImgAlt = '',
    backgroundImgSrc = '',
    backgroundImgAlt = '',
    footerListItems = null,
    textContent = '',
    footerListVariants,
    loginTitle,
    loginSubtitle,
    signUpForAccountMessage = null,
    forgotCredentials = null,
    socialMediaLoginContent = null
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "brandImgSrc", "brandImgAlt", "backgroundImgSrc", "backgroundImgAlt", "footerListItems", "textContent", "footerListVariants", "loginTitle", "loginSubtitle", "signUpForAccountMessage", "forgotCredentials", "socialMediaLoginContent"]);

  const HeaderBrand = React.createElement(React.Fragment, null, React.createElement(Brand, {
    src: brandImgSrc,
    alt: brandImgAlt
  }));
  const Header = React.createElement(LoginHeader, {
    headerBrand: HeaderBrand
  });
  const Footer = React.createElement(LoginFooter, null, React.createElement("p", null, textContent), React.createElement(List, {
    variant: footerListVariants
  }, footerListItems));
  return React.createElement(React.Fragment, null, backgroundImgSrc && React.createElement(BackgroundImage, {
    src: backgroundImgSrc,
    alt: backgroundImgAlt
  }), React.createElement(Login, _extends({
    header: Header,
    footer: Footer,
    className: css(className)
  }, props), React.createElement(LoginMainHeader, {
    title: loginTitle,
    subtitle: loginSubtitle
  }), React.createElement(LoginMainBody, null, children), (socialMediaLoginContent || forgotCredentials || signUpForAccountMessage) && React.createElement(LoginMainFooter, {
    socialMediaLoginContent: socialMediaLoginContent,
    forgotCredentials: forgotCredentials,
    signUpForAccountMessage: signUpForAccountMessage
  })));
};
LoginPage.propTypes = {
  children: _pt.node,
  className: _pt.string,
  brandImgSrc: _pt.string,
  brandImgAlt: _pt.string,
  backgroundImgSrc: _pt.oneOfType([_pt.string, _pt.any]),
  backgroundImgAlt: _pt.string,
  textContent: _pt.string,
  footerListItems: _pt.node,
  footerListVariants: _pt.any,
  loginTitle: _pt.string.isRequired,
  loginSubtitle: _pt.string,
  signUpForAccountMessage: _pt.node,
  forgotCredentials: _pt.node,
  socialMediaLoginContent: _pt.node
};
//# sourceMappingURL=LoginPage.js.map