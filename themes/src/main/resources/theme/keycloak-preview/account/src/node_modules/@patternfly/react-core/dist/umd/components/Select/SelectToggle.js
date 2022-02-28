(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Select/select", "@patternfly/react-styles/css/components/Button/button", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/caret-down-icon", "./selectConstants"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Select/select"), require("@patternfly/react-styles/css/components/Button/button"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/caret-down-icon"), require("./selectConstants"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.select, global.button, global.reactStyles, global.caretDownIcon, global.selectConstants);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _select, _button, _reactStyles, _caretDownIcon, _selectConstants) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.SelectToggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _select2 = _interopRequireDefault(_select);

  var _button2 = _interopRequireDefault(_button);

  var _caretDownIcon2 = _interopRequireDefault(_caretDownIcon);

  function _getRequireWildcardCache() {
    if (typeof WeakMap !== "function") return null;
    var cache = new WeakMap();

    _getRequireWildcardCache = function () {
      return cache;
    };

    return cache;
  }

  function _interopRequireWildcard(obj) {
    if (obj && obj.__esModule) {
      return obj;
    }

    var cache = _getRequireWildcardCache();

    if (cache && cache.has(obj)) {
      return cache.get(obj);
    }

    var newObj = {};

    if (obj != null) {
      var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor;

      for (var key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null;

          if (desc && (desc.get || desc.set)) {
            Object.defineProperty(newObj, key, desc);
          } else {
            newObj[key] = obj[key];
          }
        }
      }
    }

    newObj.default = obj;

    if (cache) {
      cache.set(obj, newObj);
    }

    return newObj;
  }

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

  function _extends() {
    _extends = Object.assign || function (target) {
      for (var i = 1; i < arguments.length; i++) {
        var source = arguments[i];

        for (var key in source) {
          if (Object.prototype.hasOwnProperty.call(source, key)) {
            target[key] = source[key];
          }
        }
      }

      return target;
    };

    return _extends.apply(this, arguments);
  }

  function _objectWithoutProperties(source, excluded) {
    if (source == null) return {};

    var target = _objectWithoutPropertiesLoose(source, excluded);

    var key, i;

    if (Object.getOwnPropertySymbols) {
      var sourceSymbolKeys = Object.getOwnPropertySymbols(source);

      for (i = 0; i < sourceSymbolKeys.length; i++) {
        key = sourceSymbolKeys[i];
        if (excluded.indexOf(key) >= 0) continue;
        if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue;
        target[key] = source[key];
      }
    }

    return target;
  }

  function _objectWithoutPropertiesLoose(source, excluded) {
    if (source == null) return {};
    var target = {};
    var sourceKeys = Object.keys(source);
    var key, i;

    for (i = 0; i < sourceKeys.length; i++) {
      key = sourceKeys[i];
      if (excluded.indexOf(key) >= 0) continue;
      target[key] = source[key];
    }

    return target;
  }

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
  }

  class SelectToggle extends React.Component {
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

        if (event.key === _selectConstants.KeyTypes.Tab && variant === _selectConstants.SelectVariant.checkbox) {
          return;
        }

        if (isExpanded && (event.key === _selectConstants.KeyTypes.Escape || event.key === _selectConstants.KeyTypes.Tab) && parentRef && parentRef.current.contains(event.target)) {
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

        if ((event.key === _selectConstants.KeyTypes.ArrowDown || event.key === _selectConstants.KeyTypes.ArrowUp) && (variant === _selectConstants.SelectVariant.typeahead || variant === _selectConstants.SelectVariant.typeaheadMulti)) {
          handleTypeaheadKeys(event.key === _selectConstants.KeyTypes.ArrowDown && 'down' || event.key === _selectConstants.KeyTypes.ArrowUp && 'up');
        }

        if (event.key === _selectConstants.KeyTypes.Enter && (variant === _selectConstants.SelectVariant.typeahead || variant === _selectConstants.SelectVariant.typeaheadMulti)) {
          if (isExpanded) {
            handleTypeaheadKeys('enter');
          } else {
            onToggle(!isExpanded);
          }
        }

        if (event.key === _selectConstants.KeyTypes.Tab && variant === _selectConstants.SelectVariant.checkbox || event.key === _selectConstants.KeyTypes.Tab && !isExpanded || event.key !== _selectConstants.KeyTypes.Enter && event.key !== _selectConstants.KeyTypes.Space || (event.key === _selectConstants.KeyTypes.Space || event.key === _selectConstants.KeyTypes.Enter) && (variant === _selectConstants.SelectVariant.typeahead || variant === _selectConstants.SelectVariant.typeaheadMulti)) {
          return;
        }

        event.preventDefault();

        if ((event.key === _selectConstants.KeyTypes.Tab || event.key === _selectConstants.KeyTypes.Enter || event.key === _selectConstants.KeyTypes.Space) && isExpanded) {
          onToggle(!isExpanded);
          onClose();
          this.toggle.current.focus();
        } else if ((event.key === _selectConstants.KeyTypes.Enter || event.key === _selectConstants.KeyTypes.Space) && !isExpanded) {
          onToggle(!isExpanded);
          onEnter();
        }
      });

      const {
        variant: _variant
      } = props;
      const isTypeahead = _variant === _selectConstants.SelectVariant.typeahead || _variant === _selectConstants.SelectVariant.typeaheadMulti;
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


      const isTypeahead = variant === _selectConstants.SelectVariant.typeahead || variant === _selectConstants.SelectVariant.typeaheadMulti || hasClearButton;
      const toggleProps = {
        id,
        'aria-labelledby': ariaLabelledBy,
        'aria-expanded': isExpanded,
        'aria-haspopup': variant !== _selectConstants.SelectVariant.checkbox && 'listbox' || null
      };
      return React.createElement(React.Fragment, null, !isTypeahead && React.createElement("button", _extends({}, props, toggleProps, {
        ref: this.toggle,
        type: type,
        className: (0, _reactStyles.css)(_select2.default.selectToggle, isFocused && _select2.default.modifiers.focus, isHovered && _select2.default.modifiers.hover, isDisabled && _select2.default.modifiers.disabled, isActive && _select2.default.modifiers.active, isPlain && _select2.default.modifiers.plain, className) // eslint-disable-next-line @typescript-eslint/no-unused-vars
        ,
        onClick: _event => {
          onToggle(!isExpanded);

          if (isExpanded) {
            onClose();
          }
        },
        onKeyDown: this.onKeyDown,
        disabled: isDisabled
      }), children, React.createElement(_caretDownIcon2.default, {
        className: (0, _reactStyles.css)(_select2.default.selectToggleArrow)
      })), isTypeahead && React.createElement("div", _extends({}, props, {
        ref: this.toggle,
        className: (0, _reactStyles.css)(_select2.default.selectToggle, isFocused && _select2.default.modifiers.focus, isHovered && _select2.default.modifiers.hover, isActive && _select2.default.modifiers.active, isDisabled && _select2.default.modifiers.disabled, isPlain && _select2.default.modifiers.plain, isTypeahead && _select2.default.modifiers.typeahead, className) // eslint-disable-next-line @typescript-eslint/no-unused-vars
        ,
        onClick: _event => {
          if (!isDisabled) {
            onToggle(true);
          }
        },
        onKeyDown: this.onKeyDown
      }), children, React.createElement("button", _extends({}, toggleProps, {
        type: type,
        className: (0, _reactStyles.css)(_button2.default.button, _select2.default.selectToggleButton, _select2.default.modifiers.plain),
        "aria-label": ariaLabelToggle,
        onClick: _event => {
          _event.stopPropagation();

          onToggle(!isExpanded);

          if (isExpanded) {
            onClose();
          }
        },
        disabled: isDisabled
      }), React.createElement(_caretDownIcon2.default, {
        className: (0, _reactStyles.css)(_select2.default.selectToggleArrow)
      }))));
    }

  }

  exports.SelectToggle = SelectToggle;

  _defineProperty(SelectToggle, "propTypes", {
    id: _propTypes2.default.string.isRequired,
    children: _propTypes2.default.node.isRequired,
    className: _propTypes2.default.string,
    isExpanded: _propTypes2.default.bool,
    onToggle: _propTypes2.default.func,
    onEnter: _propTypes2.default.func,
    onClose: _propTypes2.default.func,
    handleTypeaheadKeys: _propTypes2.default.func,
    parentRef: _propTypes2.default.any.isRequired,
    isFocused: _propTypes2.default.bool,
    isHovered: _propTypes2.default.bool,
    isActive: _propTypes2.default.bool,
    isPlain: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    type: _propTypes2.default.oneOfType([_propTypes2.default.oneOf(['reset']), _propTypes2.default.oneOf(['button']), _propTypes2.default.oneOf(['submit'])]),
    ariaLabelledBy: _propTypes2.default.string,
    ariaLabelToggle: _propTypes2.default.string,
    variant: _propTypes2.default.oneOf(['single', 'checkbox', 'typeahead', 'typeaheadmulti']),
    hasClearButton: _propTypes2.default.bool
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
});
//# sourceMappingURL=SelectToggle.js.map