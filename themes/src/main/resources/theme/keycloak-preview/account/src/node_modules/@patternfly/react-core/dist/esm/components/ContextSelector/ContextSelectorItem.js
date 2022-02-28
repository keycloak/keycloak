import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';
import { ContextSelectorContext } from './contextSelectorConstants';
export class ContextSelectorItem extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "ref", React.createRef());
  }

  componentDidMount() {
    /* eslint-disable-next-line */
    this.props.sendRef(this.props.index, this.ref.current);
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _this$props = this.props,
          {
      className,
      children,
      isHovered,
      onClick,
      isDisabled,
      index,
      sendRef
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "children", "isHovered", "onClick", "isDisabled", "index", "sendRef"]);

    return React.createElement(ContextSelectorContext.Consumer, null, ({
      onSelect
    }) => React.createElement("li", {
      role: "none"
    }, React.createElement("button", _extends({
      className: css(styles.contextSelectorMenuListItem, isDisabled && styles.modifiers.disabled, isHovered && styles.modifiers.hover, className),
      ref: this.ref,
      onClick: event => {
        if (!isDisabled) {
          onClick(event);
          onSelect(event, children);
        }
      }
    }, props), children)));
  }

}

_defineProperty(ContextSelectorItem, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  isDisabled: _pt.bool,
  isHovered: _pt.bool,
  onClick: _pt.func,
  index: _pt.number,
  sendRef: _pt.func
});

_defineProperty(ContextSelectorItem, "defaultProps", {
  children: null,
  className: '',
  isHovered: false,
  isDisabled: false,
  onClick: () => undefined,
  index: undefined,
  sendRef: () => {}
});
//# sourceMappingURL=ContextSelectorItem.js.map