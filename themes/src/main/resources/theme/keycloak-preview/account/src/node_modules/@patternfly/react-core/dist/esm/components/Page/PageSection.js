import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
export let PageSectionVariants;

(function (PageSectionVariants) {
  PageSectionVariants["default"] = "default";
  PageSectionVariants["light"] = "light";
  PageSectionVariants["dark"] = "dark";
  PageSectionVariants["darker"] = "darker";
})(PageSectionVariants || (PageSectionVariants = {}));

export let PageSectionTypes;

(function (PageSectionTypes) {
  PageSectionTypes["default"] = "default";
  PageSectionTypes["nav"] = "nav";
})(PageSectionTypes || (PageSectionTypes = {}));

export const PageSection = (_ref) => {
  let {
    className = '',
    children,
    variant = 'default',
    type = 'default',
    noPadding = false,
    noPaddingMobile = false,
    isFilled
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "children", "variant", "type", "noPadding", "noPaddingMobile", "isFilled"]);

  const variantType = {
    [PageSectionTypes.default]: styles.pageMainSection,
    [PageSectionTypes.nav]: styles.pageMainNav
  };
  const variantStyle = {
    [PageSectionVariants.default]: '',
    [PageSectionVariants.light]: styles.modifiers.light,
    [PageSectionVariants.dark]: styles.modifiers.dark_200,
    [PageSectionVariants.darker]: styles.modifiers.dark_100
  };
  return React.createElement("section", _extends({}, props, {
    className: css(variantType[type], noPadding && styles.modifiers.noPadding, noPaddingMobile && styles.modifiers.noPaddingMobile, variantStyle[variant], isFilled === false && styles.modifiers.noFill, isFilled === true && styles.modifiers.fill, className)
  }), children);
};
PageSection.propTypes = {
  children: _pt.node,
  className: _pt.string,
  variant: _pt.oneOf(['default', 'light', 'dark', 'darker']),
  type: _pt.oneOf(['default', 'nav']),
  isFilled: _pt.bool,
  noPadding: _pt.bool,
  noPaddingMobile: _pt.bool
};
//# sourceMappingURL=PageSection.js.map