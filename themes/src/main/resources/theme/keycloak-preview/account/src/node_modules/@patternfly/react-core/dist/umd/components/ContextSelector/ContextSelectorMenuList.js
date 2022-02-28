(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/ContextSelector/context-selector", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/ContextSelector/context-selector"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.contextSelector, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _contextSelector, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ContextSelectorMenuList = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

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

  class ContextSelectorMenuList extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "refsCollection", []);

      _defineProperty(this, "sendRef", (index, ref) => {
        this.refsCollection[index] = ref;
      });

      _defineProperty(this, "render", () => {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const _this$props = this.props,
              {
          className,
          isOpen,
          children
        } = _this$props,
              props = _objectWithoutProperties(_this$props, ["className", "isOpen", "children"]);

        return React.createElement("ul", _extends({
          className: (0, _reactStyles.css)(_contextSelector2.default.contextSelectorMenuList, className),
          hidden: !isOpen,
          role: "menu"
        }, props), this.extendChildren());
      });
    }

    extendChildren() {
      return React.Children.map(this.props.children, (child, index) => React.cloneElement(child, {
        sendRef: this.sendRef,
        index
      }));
    }

  }

  exports.ContextSelectorMenuList = ContextSelectorMenuList;

  _defineProperty(ContextSelectorMenuList, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    isOpen: _propTypes2.default.bool
  });

  _defineProperty(ContextSelectorMenuList, "defaultProps", {
    children: null,
    className: '',
    isOpen: true
  });
});
//# sourceMappingURL=ContextSelectorMenuList.js.map