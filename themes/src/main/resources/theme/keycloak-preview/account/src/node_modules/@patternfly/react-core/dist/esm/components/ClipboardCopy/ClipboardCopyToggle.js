import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
export const ClipboardCopyToggle = (_ref) => {
  let {
    onClick,
    className = '',
    id,
    textId,
    contentId,
    isExpanded = false
  } = _ref,
      props = _objectWithoutProperties(_ref, ["onClick", "className", "id", "textId", "contentId", "isExpanded"]);

  return React.createElement("button", _extends({
    type: "button",
    onClick: onClick,
    className: css(styles.clipboardCopyGroupToggle, className),
    id: id,
    "aria-labelledby": `${id} ${textId}`,
    "aria-controls": `${id} ${contentId}`,
    "aria-expanded": isExpanded
  }, props), React.createElement(AngleRightIcon, {
    "aria-hidden": "true",
    className: css(styles.clipboardCopyGroupToggleIcon)
  }));
};
ClipboardCopyToggle.propTypes = {
  onClick: _pt.func.isRequired,
  id: _pt.string.isRequired,
  textId: _pt.string.isRequired,
  contentId: _pt.string.isRequired,
  isExpanded: _pt.bool,
  className: _pt.string
};
//# sourceMappingURL=ClipboardCopyToggle.js.map