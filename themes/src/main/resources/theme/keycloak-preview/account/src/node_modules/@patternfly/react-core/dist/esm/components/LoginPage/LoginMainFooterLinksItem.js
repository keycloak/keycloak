import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Login/login';
import { css } from '@patternfly/react-styles';
export const LoginMainFooterLinksItem = (_ref) => {
  let {
    children = null,
    href = '',
    target = '',
    className = '',
    linkComponent = 'a',
    linkComponentProps
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "href", "target", "className", "linkComponent", "linkComponentProps"]);

  const LinkComponent = linkComponent;
  return React.createElement("li", _extends({
    className: css(styles.loginMainFooterLinksItem, className)
  }, props), React.createElement(LinkComponent, _extends({
    className: css(styles.loginMainFooterLinksItemLink),
    href: href,
    target: target
  }, linkComponentProps), children));
};
LoginMainFooterLinksItem.propTypes = {
  children: _pt.node,
  href: _pt.string,
  target: _pt.string,
  className: _pt.string,
  linkComponent: _pt.node,
  linkComponentProps: _pt.any
};
//# sourceMappingURL=LoginMainFooterLinksItem.js.map