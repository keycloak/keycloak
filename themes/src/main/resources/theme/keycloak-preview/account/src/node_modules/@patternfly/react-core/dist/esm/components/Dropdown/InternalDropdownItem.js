import _pt from "prop-types";

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { DropdownContext } from './dropdownConstants';
import { KEYHANDLER_DIRECTION } from '../../helpers/constants';
import { Tooltip } from '../Tooltip';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
export class InternalDropdownItem extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "ref", React.createRef());

    _defineProperty(this, "additionalRef", React.createRef());

    _defineProperty(this, "getInnerNode", node => node && node.childNodes && node.childNodes.length ? node.childNodes[0] : node);

    _defineProperty(this, "onKeyDown", event => {
      // Detected key press on this item, notify the menu parent so that the appropriate item can be focused
      const innerIndex = event.target === this.ref.current ? 0 : 1;

      if (!this.props.customChild) {
        event.preventDefault();
      }

      if (event.key === 'ArrowUp') {
        this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.UP);
      } else if (event.key === 'ArrowDown') {
        this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.DOWN);
      } else if (event.key === 'ArrowRight') {
        this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.RIGHT);
      } else if (event.key === 'ArrowLeft') {
        this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.LEFT);
      } else if (event.key === 'Enter' || event.key === ' ') {
        event.target.click();
        this.props.enterTriggersArrowDown && this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.DOWN);
      }
    });
  }

  componentDidMount() {
    const {
      context,
      index,
      isDisabled,
      role,
      customChild
    } = this.props;
    const customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
    context.sendRef(index, [customRef, customChild ? customRef : this.additionalRef.current], isDisabled, role === 'separator');
  }

  componentDidUpdate() {
    const {
      context,
      index,
      isDisabled,
      role,
      customChild
    } = this.props;
    const customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
    context.sendRef(index, [customRef, customChild ? customRef : this.additionalRef.current], isDisabled, role === 'separator');
  }

  extendAdditionalChildRef() {
    const {
      additionalChild
    } = this.props;
    return React.cloneElement(additionalChild, {
      ref: this.additionalRef
    });
  }

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const _this$props = this.props,
          {
      className,
      children,
      isHovered,
      context,
      onClick,
      component,
      variant,
      role,
      isDisabled,
      index,
      href,
      tooltip,
      tooltipProps,
      id,
      componentID,
      listItemClassName,
      additionalChild,
      customChild,
      enterTriggersArrowDown
    } = _this$props,
          additionalProps = _objectWithoutProperties(_this$props, ["className", "children", "isHovered", "context", "onClick", "component", "variant", "role", "isDisabled", "index", "href", "tooltip", "tooltipProps", "id", "componentID", "listItemClassName", "additionalChild", "customChild", "enterTriggersArrowDown"]);
    /* eslint-enable @typescript-eslint/no-unused-vars */


    const Component = component;
    let classes;

    if (Component === 'a') {
      additionalProps['aria-disabled'] = isDisabled;
      additionalProps.tabIndex = isDisabled ? -1 : additionalProps.tabIndex;
    } else if (Component === 'button') {
      additionalProps.disabled = isDisabled;
      additionalProps.type = additionalProps.type || 'button';
    }

    const renderWithTooltip = childNode => tooltip ? React.createElement(Tooltip, _extends({
      content: tooltip
    }, tooltipProps), childNode) : childNode;

    return React.createElement(DropdownContext.Consumer, null, ({
      onSelect,
      itemClass,
      disabledClass,
      hoverClass
    }) => {
      if (this.props.role === 'separator') {
        classes = css(variant === 'icon' && styles.modifiers.icon, className);
      } else {
        classes = css(variant === 'icon' && styles.modifiers.icon, className, isDisabled && disabledClass, isHovered && hoverClass, itemClass);
      }

      if (customChild) {
        return React.cloneElement(customChild, {
          ref: this.ref,
          onKeyDown: this.onKeyDown
        });
      }

      return React.createElement("li", {
        className: listItemClassName || null,
        role: role,
        onKeyDown: this.onKeyDown,
        onClick: event => {
          if (!isDisabled) {
            onClick(event);
            onSelect(event);
          }
        },
        id: id
      }, renderWithTooltip(React.isValidElement(component) ? React.cloneElement(component, _objectSpread({
        href,
        id: componentID,
        className: classes
      }, additionalProps)) : React.createElement(Component, _extends({}, additionalProps, {
        href: href,
        ref: this.ref,
        className: classes,
        id: componentID
      }), children)), additionalChild && this.extendAdditionalChildRef());
    });
  }

}

_defineProperty(InternalDropdownItem, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  listItemClassName: _pt.string,
  component: _pt.node,
  variant: _pt.oneOf(['item', 'icon']),
  role: _pt.string,
  isDisabled: _pt.bool,
  isHovered: _pt.bool,
  href: _pt.string,
  tooltip: _pt.node,
  tooltipProps: _pt.any,
  index: _pt.number,
  context: _pt.shape({
    keyHandler: _pt.func,
    sendRef: _pt.func
  }),
  onClick: _pt.func,
  id: _pt.string,
  componentID: _pt.string,
  additionalChild: _pt.node,
  customChild: _pt.node,
  enterTriggersArrowDown: _pt.bool
});

_defineProperty(InternalDropdownItem, "defaultProps", {
  className: '',
  isHovered: false,
  component: 'a',
  variant: 'item',
  role: 'none',
  isDisabled: false,
  tooltipProps: {},
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onClick: event => undefined,
  index: -1,
  context: {
    keyHandler: () => {},
    sendRef: () => {}
  },
  enterTriggersArrowDown: false
});
//# sourceMappingURL=InternalDropdownItem.js.map