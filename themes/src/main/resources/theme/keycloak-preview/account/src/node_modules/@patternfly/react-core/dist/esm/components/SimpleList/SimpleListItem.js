import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/SimpleList/simple-list';
import { SimpleListContext } from './SimpleList';
export class SimpleListItem extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "ref", React.createRef());
  }

  render() {
    const _this$props = this.props,
          {
      children,
      isCurrent,
      className,
      component: Component,
      componentClassName,
      componentProps,
      onClick,
      type,
      href
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "isCurrent", "className", "component", "componentClassName", "componentProps", "onClick", "type", "href"]);

    return React.createElement(SimpleListContext.Consumer, null, ({
      currentRef,
      updateCurrentRef
    }) => {
      const isButton = Component === 'button';
      const isCurrentItem = this.ref && currentRef ? currentRef.current === this.ref.current : isCurrent;
      const additionalComponentProps = isButton ? {
        type
      } : {
        tabIndex: 0,
        href
      };
      return React.createElement("li", _extends({
        className: css(className)
      }, props), React.createElement(Component, _extends({
        className: css(styles.simpleListItemLink, isCurrentItem && styles.modifiers.current, componentClassName),
        onClick: evt => {
          onClick(evt);
          updateCurrentRef(this.ref, this.props);
        },
        ref: this.ref
      }, componentProps, additionalComponentProps), children));
    });
  }

}

_defineProperty(SimpleListItem, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  component: _pt.oneOf(['button', 'a']),
  componentClassName: _pt.string,
  componentProps: _pt.any,
  isCurrent: _pt.bool,
  onClick: _pt.func,
  type: _pt.oneOf(['button', 'submit', 'reset']),
  href: _pt.string
});

_defineProperty(SimpleListItem, "defaultProps", {
  children: null,
  className: '',
  isCurrent: false,
  component: 'button',
  componentClassName: '',
  type: 'button',
  href: '',
  onClick: () => {}
});
//# sourceMappingURL=SimpleListItem.js.map