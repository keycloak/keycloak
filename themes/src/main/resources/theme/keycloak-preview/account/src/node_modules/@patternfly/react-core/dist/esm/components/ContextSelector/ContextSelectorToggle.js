import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import CaretDownIcon from '@patternfly/react-icons/dist/js/icons/caret-down-icon';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';
import { KEY_CODES } from '../../helpers/constants';
export class ContextSelectorToggle extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "toggle", React.createRef());

    _defineProperty(this, "componentDidMount", () => {
      document.addEventListener('mousedown', this.onDocClick);
      document.addEventListener('touchstart', this.onDocClick);
      document.addEventListener('keydown', this.onEscPress);
    });

    _defineProperty(this, "componentWillUnmount", () => {
      document.removeEventListener('mousedown', this.onDocClick);
      document.removeEventListener('touchstart', this.onDocClick);
      document.removeEventListener('keydown', this.onEscPress);
    });

    _defineProperty(this, "onDocClick", event => {
      const {
        isOpen,
        parentRef,
        onToggle
      } = this.props;

      if (isOpen && parentRef && !parentRef.contains(event.target)) {
        onToggle(null, false);
        this.toggle.current.focus();
      }
    });

    _defineProperty(this, "onEscPress", event => {
      const {
        isOpen,
        parentRef,
        onToggle
      } = this.props;
      const keyCode = event.keyCode || event.which;

      if (isOpen && keyCode === KEY_CODES.ESCAPE_KEY && parentRef && parentRef.contains(event.target)) {
        onToggle(null, false);
        this.toggle.current.focus();
      }
    });

    _defineProperty(this, "onKeyDown", event => {
      const {
        isOpen,
        onToggle,
        onEnter
      } = this.props;

      if (event.keyCode === KEY_CODES.TAB && !isOpen || event.key !== KEY_CODES.ENTER) {
        return;
      }

      event.preventDefault();

      if ((event.keyCode === KEY_CODES.TAB || event.keyCode === KEY_CODES.ENTER || event.key !== KEY_CODES.SPACE) && isOpen) {
        onToggle(null, !isOpen);
      } else if ((event.keyCode === KEY_CODES.ENTER || event.key === ' ') && !isOpen) {
        onToggle(null, !isOpen);
        onEnter();
      }
    });
  }

  render() {
    const _this$props = this.props,
          {
      className,
      toggleText,
      isOpen,
      isFocused,
      isActive,
      isHovered,
      onToggle,
      id,

      /* eslint-disable @typescript-eslint/no-unused-vars */
      onEnter,
      parentRef
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "toggleText", "isOpen", "isFocused", "isActive", "isHovered", "onToggle", "id", "onEnter", "parentRef"]);

    return React.createElement("button", _extends({}, props, {
      id: id,
      ref: this.toggle,
      className: css(styles.contextSelectorToggle, isFocused && styles.modifiers.focus, isHovered && styles.modifiers.hover, isActive && styles.modifiers.active, className),
      type: "button",
      onClick: event => onToggle(event, !isOpen),
      "aria-expanded": isOpen,
      onKeyDown: this.onKeyDown
    }), React.createElement("span", {
      className: css(styles.contextSelectorToggleText)
    }, toggleText), React.createElement(CaretDownIcon, {
      className: css(styles.contextSelectorToggleIcon),
      "aria-hidden": true
    }));
  }

}

_defineProperty(ContextSelectorToggle, "propTypes", {
  id: _pt.string.isRequired,
  className: _pt.string,
  toggleText: _pt.string,
  isOpen: _pt.bool,
  onToggle: _pt.func,
  onEnter: _pt.func,
  parentRef: _pt.any,
  isFocused: _pt.bool,
  isHovered: _pt.bool,
  isActive: _pt.bool
});

_defineProperty(ContextSelectorToggle, "defaultProps", {
  className: '',
  toggleText: '',
  isOpen: false,
  onEnter: () => undefined,
  parentRef: null,
  isFocused: false,
  isHovered: false,
  isActive: false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onToggle: (event, value) => undefined
});
//# sourceMappingURL=ContextSelectorToggle.js.map