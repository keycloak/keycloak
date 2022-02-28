import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AboutModalBox/about-modal-box';
import contentStyles from '@patternfly/react-styles/css/components/Content/content';
export const AboutModalBoxContent = (_ref) => {
  let {
    children,
    className = '',
    trademark,
    id,
    noAboutModalBoxContentContainer = false
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "trademark", "id", "noAboutModalBoxContentContainer"]);

  return React.createElement("div", _extends({
    className: css(styles.aboutModalBoxContent, className),
    id: id
  }, props), React.createElement("div", {
    className: css('pf-c-about-modal-box__body')
  }, noAboutModalBoxContentContainer ? children : React.createElement("div", {
    className: css(contentStyles.content)
  }, children)), React.createElement("p", {
    className: css(styles.aboutModalBoxStrapline)
  }, trademark));
};
AboutModalBoxContent.propTypes = {
  children: _pt.node.isRequired,
  className: _pt.string,
  id: _pt.string.isRequired,
  trademark: _pt.string.isRequired,
  noAboutModalBoxContentContainer: _pt.bool
};
//# sourceMappingURL=AboutModalBoxContent.js.map