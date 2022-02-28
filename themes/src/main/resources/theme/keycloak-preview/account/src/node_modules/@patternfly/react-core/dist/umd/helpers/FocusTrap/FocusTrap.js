(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "focus-trap"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("focus-trap"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.focusTrap);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _focusTrap) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.FocusTrap = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _focusTrap2 = _interopRequireDefault(_focusTrap);

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

  function ownKeys(object, enumerableOnly) {
    var keys = Object.keys(object);

    if (Object.getOwnPropertySymbols) {
      var symbols = Object.getOwnPropertySymbols(object);
      if (enumerableOnly) symbols = symbols.filter(function (sym) {
        return Object.getOwnPropertyDescriptor(object, sym).enumerable;
      });
      keys.push.apply(keys, symbols);
    }

    return keys;
  }

  function _objectSpread(target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i] != null ? arguments[i] : {};

      if (i % 2) {
        ownKeys(source, true).forEach(function (key) {
          _defineProperty(target, key, source[key]);
        });
      } else if (Object.getOwnPropertyDescriptors) {
        Object.defineProperties(target, Object.getOwnPropertyDescriptors(source));
      } else {
        ownKeys(source).forEach(function (key) {
          Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key));
        });
      }
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

  class FocusTrap extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "divRef", React.createRef());

      if (typeof document !== 'undefined') {
        this.previouslyFocusedElement = document.activeElement;
      }
    }

    componentDidMount() {
      // We need to hijack the returnFocusOnDeactivate option,
      // because React can move focus into the element before we arrived at
      // this lifecycle hook (e.g. with autoFocus inputs). So the component
      // captures the previouslyFocusedElement in componentWillMount,
      // then (optionally) returns focus to it in componentWillUnmount.
      this.focusTrap = (0, _focusTrap2.default)(this.divRef.current, _objectSpread({}, this.props.focusTrapOptions, {
        returnFocusOnDeactivate: false
      }));

      if (this.props.active) {
        this.focusTrap.activate();
      }

      if (this.props.paused) {
        this.focusTrap.pause();
      }
    }

    componentDidUpdate(prevProps) {
      if (prevProps.active && !this.props.active) {
        const {
          returnFocusOnDeactivate
        } = this.props.focusTrapOptions;
        const returnFocus = returnFocusOnDeactivate || false;
        const config = {
          returnFocus
        };
        this.focusTrap.deactivate(config);
      } else if (!prevProps.active && this.props.active) {
        this.focusTrap.activate();
      }

      if (prevProps.paused && !this.props.paused) {
        this.focusTrap.unpause();
      } else if (!prevProps.paused && this.props.paused) {
        this.focusTrap.pause();
      }
    }

    componentWillUnmount() {
      this.focusTrap.deactivate();

      if (this.props.focusTrapOptions.returnFocusOnDeactivate !== false && this.previouslyFocusedElement && this.previouslyFocusedElement.focus) {
        this.previouslyFocusedElement.focus();
      }
    }

    render() {
      return React.createElement("div", {
        ref: this.divRef,
        className: this.props.className
      }, this.props.children);
    }

  }

  exports.FocusTrap = FocusTrap;

  _defineProperty(FocusTrap, "propTypes", {
    children: _propTypes2.default.node.isRequired,
    className: _propTypes2.default.string,
    active: _propTypes2.default.bool,
    paused: _propTypes2.default.bool,
    focusTrapOptions: _propTypes2.default.any
  });

  _defineProperty(FocusTrap, "defaultProps", {
    active: true,
    paused: false,
    focusTrapOptions: {}
  });
});
//# sourceMappingURL=FocusTrap.js.map