(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/SimpleList/simple-list", "./SimpleListGroup"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/SimpleList/simple-list"), require("./SimpleListGroup"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.simpleList, global.SimpleListGroup);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _simpleList, _SimpleListGroup) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.SimpleList = exports.SimpleListContext = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _simpleList2 = _interopRequireDefault(_simpleList);

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

  const SimpleListContext = exports.SimpleListContext = React.createContext({});

  class SimpleList extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "state", {
        currentRef: null
      });

      _defineProperty(this, "handleCurrentUpdate", (newCurrentRef, itemProps) => {
        this.setState({
          currentRef: newCurrentRef
        });
        const {
          onSelect
        } = this.props;
        onSelect && onSelect(newCurrentRef, itemProps);
      });
    }

    componentDidMount() {
      if (!SimpleList.hasWarnBeta && process.env.NODE_ENV !== 'production') {
        // eslint-disable-next-line no-console
        console.warn('This component is in beta and subject to change.');
        SimpleList.hasWarnBeta = true;
      }
    }

    render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const _this$props = this.props,
            {
        children,
        className,
        onSelect
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["children", "className", "onSelect"]);

      let isGrouped = false;

      if (children) {
        isGrouped = React.Children.toArray(children)[0].type === _SimpleListGroup.SimpleListGroup;
      }

      return React.createElement(SimpleListContext.Provider, {
        value: {
          currentRef: this.state.currentRef,
          updateCurrentRef: this.handleCurrentUpdate
        }
      }, React.createElement("div", _extends({
        className: (0, _reactStyles.css)(_simpleList2.default.simpleList, className)
      }, props, isGrouped && {
        role: 'list'
      }), isGrouped && children, !isGrouped && React.createElement("ul", null, children)));
    }

  }

  exports.SimpleList = SimpleList;

  _defineProperty(SimpleList, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    onSelect: _propTypes2.default.func
  });

  _defineProperty(SimpleList, "hasWarnBeta", false);

  _defineProperty(SimpleList, "defaultProps", {
    children: null,
    className: ''
  });
});
//# sourceMappingURL=SimpleList.js.map