import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy';
import { css } from '@patternfly/react-styles';
export class ClipboardCopyExpanded extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    const _this$props = this.props,
          {
      className,
      children,
      onChange,
      isReadOnly,
      isCode
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "children", "onChange", "isReadOnly", "isCode"]);

    return React.createElement("div", _extends({
      suppressContentEditableWarning: true,
      className: css(styles.clipboardCopyExpandableContent, className),
      onInput: e => onChange(e.target.innerText, e),
      contentEditable: !isReadOnly
    }, props), isCode ? React.createElement("pre", null, children) : children);
  }

}

_defineProperty(ClipboardCopyExpanded, "propTypes", {
  className: _pt.string,
  children: _pt.node.isRequired,
  onChange: _pt.func,
  isReadOnly: _pt.bool,
  isCode: _pt.bool
});

_defineProperty(ClipboardCopyExpanded, "defaultProps", {
  onChange: () => undefined,
  className: '',
  isReadOnly: false,
  isCode: false
});
//# sourceMappingURL=ClipboardCopyExpanded.js.map