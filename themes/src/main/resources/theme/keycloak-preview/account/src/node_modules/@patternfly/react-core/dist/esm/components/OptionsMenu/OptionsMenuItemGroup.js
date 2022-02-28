import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { css } from '@patternfly/react-styles';
export const OptionsMenuItemGroup = (_ref) => {
  let {
    className = '',
    ariaLabel = '',
    groupTitle = '',
    children = null,
    hasSeparator = false
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "ariaLabel", "groupTitle", "children", "hasSeparator"]);

  return React.createElement("section", _extends({}, props, {
    className: css(styles.optionsMenuGroup)
  }), groupTitle && React.createElement("h1", {
    className: css(styles.optionsMenuGroupTitle)
  }, groupTitle), React.createElement("ul", {
    className: className,
    "aria-label": ariaLabel
  }, children, hasSeparator && React.createElement("li", {
    className: css(styles.optionsMenuSeparator),
    role: "separator"
  })));
};
OptionsMenuItemGroup.propTypes = {
  children: _pt.node,
  className: _pt.string,
  ariaLabel: _pt.string,
  groupTitle: _pt.oneOfType([_pt.string, _pt.node]),
  hasSeparator: _pt.bool
};
//# sourceMappingURL=OptionsMenuItemGroup.js.map