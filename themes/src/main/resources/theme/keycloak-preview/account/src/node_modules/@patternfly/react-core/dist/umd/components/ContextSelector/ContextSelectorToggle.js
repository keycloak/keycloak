(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-icons/dist/js/icons/caret-down-icon", "@patternfly/react-styles/css/components/ContextSelector/context-selector", "@patternfly/react-styles", "../../helpers/constants"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-icons/dist/js/icons/caret-down-icon"), require("@patternfly/react-styles/css/components/ContextSelector/context-selector"), require("@patternfly/react-styles"), require("../../helpers/constants"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.caretDownIcon, global.contextSelector, global.reactStyles, global.constants);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _caretDownIcon, _contextSelector, _reactStyles, _constants) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ContextSelectorToggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _caretDownIcon2 = _interopRequireDefault(_caretDownIcon);

  var _contextSelector2 = _interopRequireDefault(_contextSelector);

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

  class ContextSelectorToggle extends React.Component {
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

        if (isOpen && keyCode === _constants.KEY_CODES.ESCAPE_KEY && parentRef && parentRef.contains(event.target)) {
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

        if (event.keyCode === _constants.KEY_CODES.TAB && !isOpen || event.key !== _constants.KEY_CODES.ENTER) {
          return;
        }

        event.preventDefault();

        if ((event.keyCode === _constants.KEY_CODES.TAB || event.keyCode === _constants.KEY_CODES.ENTER || event.key !== _constants.KEY_CODES.SPACE) && isOpen) {
          onToggle(null, !isOpen);
        } else if ((event.keyCode === _constants.KEY_CODES.ENTER || event.key === ' ') && !isOpen) {
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
        className: (0, _reactStyles.css)(_contextSelector2.default.contextSelectorToggle, isFocused && _contextSelector2.default.modifiers.focus, isHovered && _contextSelector2.default.modifiers.hover, isActive && _contextSelector2.default.modifiers.active, className),
        type: "button",
        onClick: event => onToggle(event, !isOpen),
        "aria-expanded": isOpen,
        onKeyDown: this.onKeyDown
      }), React.createElement("span", {
        className: (0, _reactStyles.css)(_contextSelector2.default.contextSelectorToggleText)
      }, toggleText), React.createElement(_caretDownIcon2.default, {
        className: (0, _reactStyles.css)(_contextSelector2.default.contextSelectorToggleIcon),
        "aria-hidden": true
      }));
    }

  }

  exports.ContextSelectorToggle = ContextSelectorToggle;

  _defineProperty(ContextSelectorToggle, "propTypes", {
    id: _propTypes2.default.string.isRequired,
    className: _propTypes2.default.string,
    toggleText: _propTypes2.default.string,
    isOpen: _propTypes2.default.bool,
    onToggle: _propTypes2.default.func,
    onEnter: _propTypes2.default.func,
    parentRef: _propTypes2.default.any,
    isFocused: _propTypes2.default.bool,
    isHovered: _propTypes2.default.bool,
    isActive: _propTypes2.default.bool
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
});
//# sourceMappingURL=ContextSelectorToggle.js.map