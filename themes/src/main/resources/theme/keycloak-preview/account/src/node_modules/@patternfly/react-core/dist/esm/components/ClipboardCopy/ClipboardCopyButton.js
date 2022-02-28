import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy';
import { css } from '@patternfly/react-styles';
import CopyIcon from '@patternfly/react-icons/dist/js/icons/copy-icon';
import { Tooltip } from '../Tooltip';
export const ClipboardCopyButton = (_ref) => {
  let {
    onClick,
    className = '',
    exitDelay = 100,
    entryDelay = 100,
    maxWidth = '100px',
    position = 'top',
    'aria-label': ariaLabel = 'Copyable input',
    id,
    textId,
    children
  } = _ref,
      props = _objectWithoutProperties(_ref, ["onClick", "className", "exitDelay", "entryDelay", "maxWidth", "position", "aria-label", "id", "textId", "children"]);

  return React.createElement(Tooltip, {
    trigger: "mouseenter focus click",
    exitDelay: exitDelay,
    entryDelay: entryDelay,
    maxWidth: maxWidth,
    position: position,
    content: React.createElement("div", null, children)
  }, React.createElement("button", _extends({
    type: "button",
    onClick: onClick,
    className: css(styles.clipboardCopyGroupCopy, className),
    "aria-label": ariaLabel,
    id: id,
    "aria-labelledby": `${id} ${textId}`
  }, props), React.createElement(CopyIcon, null)));
};
ClipboardCopyButton.propTypes = {
  onClick: _pt.func.isRequired,
  children: _pt.node.isRequired,
  id: _pt.string.isRequired,
  textId: _pt.string.isRequired,
  className: _pt.string,
  exitDelay: _pt.number,
  entryDelay: _pt.number,
  maxWidth: _pt.string,
  position: _pt.oneOf(['auto', 'top', 'bottom', 'left', 'right']),
  'aria-label': _pt.string
};
//# sourceMappingURL=ClipboardCopyButton.js.map