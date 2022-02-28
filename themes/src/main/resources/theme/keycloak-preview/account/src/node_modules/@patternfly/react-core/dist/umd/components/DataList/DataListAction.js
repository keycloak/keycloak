(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/DataList/data-list"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/DataList/data-list"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.dataList);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _dataList) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DataListAction = exports.DataListActionVisibility = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _dataList2 = _interopRequireDefault(_dataList);

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

  const visibilityModifiers = (0, _reactStyles.pickProperties)(_dataList2.default.modifiers, ['hidden', 'hiddenOnSm', 'hiddenOnMd', 'hiddenOnLg', 'hiddenOnXl', 'hiddenOn_2xl', 'visibleOnSm', 'visibleOnMd', 'visibleOnLg', 'visibleOnXl', 'visibleOn_2xl']); // eslint-disable-next-line @typescript-eslint/interface-name-prefix

  const DataListActionVisibility = exports.DataListActionVisibility = Object.keys(visibilityModifiers).map(key => [key.replace('_2xl', '2Xl'), visibilityModifiers[key]]).reduce((acc, curr) => _objectSpread({}, acc, {
    [curr[0]]: curr[1]
  }), {});

  class DataListAction extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "onToggle", isOpen => {
        this.setState({
          isOpen
        });
      });

      _defineProperty(this, "onSelect", event => {
        this.setState(prevState => ({
          isOpen: !prevState.isOpen
        }));
      });

      this.state = {
        isOpen: false
      };
    }

    render() {
      const _this$props = this.props,
            {
        children,
        className,

        /* eslint-disable @typescript-eslint/no-unused-vars */
        id,
        'aria-label': ariaLabel,
        'aria-labelledby': ariaLabelledBy
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["children", "className", "id", "aria-label", "aria-labelledby"]);

      return React.createElement("div", _extends({
        className: (0, _reactStyles.css)(_dataList2.default.dataListItemAction, className)
      }, props), children);
    }

  }

  exports.DataListAction = DataListAction;

  _defineProperty(DataListAction, "propTypes", {
    children: _propTypes2.default.node.isRequired,
    className: _propTypes2.default.string,
    id: _propTypes2.default.string.isRequired,
    'aria-labelledby': _propTypes2.default.string.isRequired,
    'aria-label': _propTypes2.default.string.isRequired
  });

  _defineProperty(DataListAction, "defaultProps", {
    className: ''
  });
});
//# sourceMappingURL=DataListAction.js.map