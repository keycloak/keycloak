(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Dropdown/dropdown", "./dropdownConstants", "@patternfly/react-styles", "../../helpers/constants"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Dropdown/dropdown"), require("./dropdownConstants"), require("@patternfly/react-styles"), require("../../helpers/constants"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.dropdown, global.dropdownConstants, global.reactStyles, global.constants);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _dropdown, _dropdownConstants, _reactStyles, _constants) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Toggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _dropdown2 = _interopRequireDefault(_dropdown);

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

  class Toggle extends React.Component {
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

        if (this.props.isOpen && (keyCode === _constants.KEY_CODES.ESCAPE_KEY || event.key === 'Tab') && parentRef && parentRef.current && parentRef.current.contains(event.target)) {
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

      return React.createElement(_dropdownConstants.DropdownContext.Consumer, null, ({
        toggleClass
      }) => React.createElement("button", _extends({}, props, {
        id: id,
        ref: this.buttonRef,
        className: (0, _reactStyles.css)(isSplitButton ? _dropdown2.default.dropdownToggleButton : toggleClass || _dropdown2.default.dropdownToggle, isFocused && _dropdown2.default.modifiers.focus, isHovered && _dropdown2.default.modifiers.hover, isActive && _dropdown2.default.modifiers.active, isPlain && _dropdown2.default.modifiers.plain, isPrimary && _dropdown2.default.modifiers.primary, className),
        type: type || 'button',
        onClick: event => onToggle(!isOpen, event),
        "aria-expanded": isOpen,
        "aria-haspopup": ariaHasPopup,
        onKeyDown: event => this.onKeyDown(event),
        disabled: isDisabled
      }), children));
    }

  }

  exports.Toggle = Toggle;

  _defineProperty(Toggle, "propTypes", {
    id: _propTypes2.default.string.isRequired,
    type: _propTypes2.default.oneOf(['button', 'submit', 'reset']),
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    isOpen: _propTypes2.default.bool,
    onToggle: _propTypes2.default.func,
    onEnter: _propTypes2.default.func,
    parentRef: _propTypes2.default.any,
    isFocused: _propTypes2.default.bool,
    isHovered: _propTypes2.default.bool,
    isActive: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    isPlain: _propTypes2.default.bool,
    isPrimary: _propTypes2.default.bool,
    isSplitButton: _propTypes2.default.bool,
    ariaHasPopup: _propTypes2.default.oneOfType([_propTypes2.default.bool, _propTypes2.default.oneOf(['listbox']), _propTypes2.default.oneOf(['menu']), _propTypes2.default.oneOf(['dialog']), _propTypes2.default.oneOf(['grid']), _propTypes2.default.oneOf(['listbox']), _propTypes2.default.oneOf(['tree'])]),
    bubbleEvent: _propTypes2.default.bool
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
});
//# sourceMappingURL=Toggle.js.map