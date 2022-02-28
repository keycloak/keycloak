import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { DropdownContext } from './dropdownConstants';
import { css } from '@patternfly/react-styles';
import { KEY_CODES } from '../../helpers/constants';
export class Toggle extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "buttonRef", React.createRef());

    _defineProperty(this, "componentDidMount", () => {
      document.addEventListener('mousedown', event => this.onDocClick(event));
      document.addEventListener('touchstart', event => this.onDocClick(event));
      document.addEventListener('keydown', event => this.onEscPress(event));
    });

    _defineProperty(this, "componentWillUnmount", () => {
      document.removeEventListener('mousedown', event => this.onDocClick(event));
      document.removeEventListener('touchstart', event => this.onDocClick(event));
      document.removeEventListener('keydown', event => this.onEscPress(event));
    });

    _defineProperty(this, "onDocClick", event => {
      if (this.props.isOpen && this.props.parentRef && this.props.parentRef.current && !this.props.parentRef.current.contains(event.target)) {
        this.props.onToggle(false, event);
        this.buttonRef.current.focus();
      }
    });

    _defineProperty(this, "onEscPress", event => {
      const {
        parentRef
      } = this.props;
      const keyCode = event.keyCode || event.which;

      if (this.props.isOpen && (keyCode === KEY_CODES.ESCAPE_KEY || event.key === 'Tab') && parentRef && parentRef.current && parentRef.current.contains(event.target)) {
        this.props.onToggle(false, event);
        this.buttonRef.current.focus();
      }
    });

    _defineProperty(this, "onKeyDown", event => {
      if (event.key === 'Tab' && !this.props.isOpen) {
        return;
      }

      if (!this.props.bubbleEvent) {
        event.stopPropagation();
      }

      event.preventDefault();

      if ((event.key === 'Tab' || event.key === 'Enter' || event.key === ' ') && this.props.isOpen) {
        this.props.onToggle(!this.props.isOpen, event);
      } else if ((event.key === 'Enter' || event.key === ' ') && !this.props.isOpen) {
        this.props.onToggle(!this.props.isOpen, event);
        this.props.onEnter();
      }
    });
  }

  render() {
    const _this$props = this.props,
          {
      className,
      children,
      isOpen,
      isFocused,
      isActive,
      isHovered,
      isDisabled,
      isPlain,
      isPrimary,
      isSplitButton,
      ariaHasPopup,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      bubbleEvent,
      onToggle,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onEnter,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      parentRef,
      id,
      type
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "children", "isOpen", "isFocused", "isActive", "isHovered", "isDisabled", "isPlain", "isPrimary", "isSplitButton", "ariaHasPopup", "bubbleEvent", "onToggle", "onEnter", "parentRef", "id", "type"]);

    return React.createElement(DropdownContext.Consumer, null, ({
      toggleClass
    }) => React.createElement("button", _extends({}, props, {
      id: id,
      ref: this.buttonRef,
      className: css(isSplitButton ? styles.dropdownToggleButton : toggleClass || styles.dropdownToggle, isFocused && styles.modifiers.focus, isHovered && styles.modifiers.hover, isActive && styles.modifiers.active, isPlain && styles.modifiers.plain, isPrimary && styles.modifiers.primary, className),
      type: type || 'button',
      onClick: event => onToggle(!isOpen, event),
      "aria-expanded": isOpen,
      "aria-haspopup": ariaHasPopup,
      onKeyDown: event => this.onKeyDown(event),
      disabled: isDisabled
    }), children));
  }

}

_defineProperty(Toggle, "propTypes", {
  id: _pt.string.isRequired,
  type: _pt.oneOf(['button', 'submit', 'reset']),
  children: _pt.node,
  className: _pt.string,
  isOpen: _pt.bool,
  onToggle: _pt.func,
  onEnter: _pt.func,
  parentRef: _pt.any,
  isFocused: _pt.bool,
  isHovered: _pt.bool,
  isActive: _pt.bool,
  isDisabled: _pt.bool,
  isPlain: _pt.bool,
  isPrimary: _pt.bool,
  isSplitButton: _pt.bool,
  ariaHasPopup: _pt.oneOfType([_pt.bool, _pt.oneOf(['listbox']), _pt.oneOf(['menu']), _pt.oneOf(['dialog']), _pt.oneOf(['grid']), _pt.oneOf(['listbox']), _pt.oneOf(['tree'])]),
  bubbleEvent: _pt.bool
});

_defineProperty(Toggle, "defaultProps", {
  className: '',
  isOpen: false,
  isFocused: false,
  isHovered: false,
  isActive: false,
  isDisabled: false,
  isPlain: false,
  isPrimary: false,
  isSplitButton: false,
  onToggle: () => {},
  onEnter: () => {},
  bubbleEvent: false
});
//# sourceMappingURL=Toggle.js.map