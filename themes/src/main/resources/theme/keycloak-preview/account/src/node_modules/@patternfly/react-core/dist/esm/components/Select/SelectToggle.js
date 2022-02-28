import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import CaretDownIcon from '@patternfly/react-icons/dist/js/icons/caret-down-icon';
import { KeyTypes, SelectVariant } from './selectConstants';
export class SelectToggle extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "onDocClick", event => {
      const {
        parentRef,
        isExpanded,
        onToggle,
        onClose
      } = this.props;

      if (isExpanded && parentRef && !parentRef.current.contains(event.target)) {
        onToggle(false);
        onClose();
        this.toggle.current.focus();
      }
    });

    _defineProperty(this, "onEscPress", event => {
      const {
        parentRef,
        isExpanded,
        variant,
        onToggle,
        onClose
      } = this.props;

      if (event.key === KeyTypes.Tab && variant === SelectVariant.checkbox) {
        return;
      }

      if (isExpanded && (event.key === KeyTypes.Escape || event.key === KeyTypes.Tab) && parentRef && parentRef.current.contains(event.target)) {
        onToggle(false);
        onClose();
        this.toggle.current.focus();
      }
    });

    _defineProperty(this, "onKeyDown", event => {
      const {
        isExpanded,
        onToggle,
        variant,
        onClose,
        onEnter,
        handleTypeaheadKeys
      } = this.props;

      if ((event.key === KeyTypes.ArrowDown || event.key === KeyTypes.ArrowUp) && (variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti)) {
        handleTypeaheadKeys(event.key === KeyTypes.ArrowDown && 'down' || event.key === KeyTypes.ArrowUp && 'up');
      }

      if (event.key === KeyTypes.Enter && (variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti)) {
        if (isExpanded) {
          handleTypeaheadKeys('enter');
        } else {
          onToggle(!isExpanded);
        }
      }

      if (event.key === KeyTypes.Tab && variant === SelectVariant.checkbox || event.key === KeyTypes.Tab && !isExpanded || event.key !== KeyTypes.Enter && event.key !== KeyTypes.Space || (event.key === KeyTypes.Space || event.key === KeyTypes.Enter) && (variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti)) {
        return;
      }

      event.preventDefault();

      if ((event.key === KeyTypes.Tab || event.key === KeyTypes.Enter || event.key === KeyTypes.Space) && isExpanded) {
        onToggle(!isExpanded);
        onClose();
        this.toggle.current.focus();
      } else if ((event.key === KeyTypes.Enter || event.key === KeyTypes.Space) && !isExpanded) {
        onToggle(!isExpanded);
        onEnter();
      }
    });

    const {
      variant: _variant
    } = props;
    const isTypeahead = _variant === SelectVariant.typeahead || _variant === SelectVariant.typeaheadMulti;
    this.toggle = isTypeahead ? React.createRef() : React.createRef();
  }

  componentDidMount() {
    document.addEventListener('mousedown', this.onDocClick);
    document.addEventListener('touchstart', this.onDocClick);
    document.addEventListener('keydown', this.onEscPress);
  }

  componentWillUnmount() {
    document.removeEventListener('mousedown', this.onDocClick);
    document.removeEventListener('touchstart', this.onDocClick);
    document.removeEventListener('keydown', this.onEscPress);
  }

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const _this$props = this.props,
          {
      className,
      children,
      isExpanded,
      isFocused,
      isActive,
      isHovered,
      isPlain,
      isDisabled,
      variant,
      onToggle,
      onEnter,
      onClose,
      handleTypeaheadKeys,
      parentRef,
      id,
      type,
      hasClearButton,
      ariaLabelledBy,
      ariaLabelToggle
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "children", "isExpanded", "isFocused", "isActive", "isHovered", "isPlain", "isDisabled", "variant", "onToggle", "onEnter", "onClose", "handleTypeaheadKeys", "parentRef", "id", "type", "hasClearButton", "ariaLabelledBy", "ariaLabelToggle"]);
    /* eslint-enable @typescript-eslint/no-unused-vars */


    const isTypeahead = variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti || hasClearButton;
    const toggleProps = {
      id,
      'aria-labelledby': ariaLabelledBy,
      'aria-expanded': isExpanded,
      'aria-haspopup': variant !== SelectVariant.checkbox && 'listbox' || null
    };
    return React.createElement(React.Fragment, null, !isTypeahead && React.createElement("button", _extends({}, props, toggleProps, {
      ref: this.toggle,
      type: type,
      className: css(styles.selectToggle, isFocused && styles.modifiers.focus, isHovered && styles.modifiers.hover, isDisabled && styles.modifiers.disabled, isActive && styles.modifiers.active, isPlain && styles.modifiers.plain, className) // eslint-disable-next-line @typescript-eslint/no-unused-vars
      ,
      onClick: _event => {
        onToggle(!isExpanded);

        if (isExpanded) {
          onClose();
        }
      },
      onKeyDown: this.onKeyDown,
      disabled: isDisabled
    }), children, React.createElement(CaretDownIcon, {
      className: css(styles.selectToggleArrow)
    })), isTypeahead && React.createElement("div", _extends({}, props, {
      ref: this.toggle,
      className: css(styles.selectToggle, isFocused && styles.modifiers.focus, isHovered && styles.modifiers.hover, isActive && styles.modifiers.active, isDisabled && styles.modifiers.disabled, isPlain && styles.modifiers.plain, isTypeahead && styles.modifiers.typeahead, className) // eslint-disable-next-line @typescript-eslint/no-unused-vars
      ,
      onClick: _event => {
        if (!isDisabled) {
          onToggle(true);
        }
      },
      onKeyDown: this.onKeyDown
    }), children, React.createElement("button", _extends({}, toggleProps, {
      type: type,
      className: css(buttonStyles.button, styles.selectToggleButton, styles.modifiers.plain),
      "aria-label": ariaLabelToggle,
      onClick: _event => {
        _event.stopPropagation();

        onToggle(!isExpanded);

        if (isExpanded) {
          onClose();
        }
      },
      disabled: isDisabled
    }), React.createElement(CaretDownIcon, {
      className: css(styles.selectToggleArrow)
    }))));
  }

}

_defineProperty(SelectToggle, "propTypes", {
  id: _pt.string.isRequired,
  children: _pt.node.isRequired,
  className: _pt.string,
  isExpanded: _pt.bool,
  onToggle: _pt.func,
  onEnter: _pt.func,
  onClose: _pt.func,
  handleTypeaheadKeys: _pt.func,
  parentRef: _pt.any.isRequired,
  isFocused: _pt.bool,
  isHovered: _pt.bool,
  isActive: _pt.bool,
  isPlain: _pt.bool,
  isDisabled: _pt.bool,
  type: _pt.oneOfType([_pt.oneOf(['reset']), _pt.oneOf(['button']), _pt.oneOf(['submit'])]),
  ariaLabelledBy: _pt.string,
  ariaLabelToggle: _pt.string,
  variant: _pt.oneOf(['single', 'checkbox', 'typeahead', 'typeaheadmulti']),
  hasClearButton: _pt.bool
});

_defineProperty(SelectToggle, "defaultProps", {
  className: '',
  isExpanded: false,
  isFocused: false,
  isHovered: false,
  isActive: false,
  isPlain: false,
  isDisabled: false,
  hasClearButton: false,
  variant: 'single',
  ariaLabelledBy: '',
  ariaLabelToggle: '',
  type: 'button',
  onToggle: () => {},
  onEnter: () => {},
  onClose: () => {}
});
//# sourceMappingURL=SelectToggle.js.map