(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/SimpleList/simple-list", "./SimpleList"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/SimpleList/simple-list"), require("./SimpleList"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.simpleList, global.SimpleList);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _simpleList, _SimpleList) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.SimpleListItem = undefined;

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

  class SimpleListItem extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "ref", React.createRef());
    }

    render() {
      const _this$props = this.props,
            {
        children,
        isCurrent,
        className,
        component: Component,
        componentClassName,
        componentProps,
        onClick,
        type,
        href
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["children", "isCurrent", "className", "component", "componentClassName", "componentProps", "onClick", "type", "href"]);

      return React.createElement(_SimpleList.SimpleListContext.Consumer, null, ({
        currentRef,
        updateCurrentRef
      }) => {
        const isButton = Component === 'button';
        const isCurrentItem = this.ref && currentRef ? currentRef.current === this.ref.current : isCurrent;
        const additionalComponentProps = isButton ? {
          type
        } : {
          tabIndex: 0,
          href
        };
        return React.createElement("li", _extends({
          className: (0, _reactStyles.css)(className)
        }, props), React.createElement(Component, _extends({
          className: (0, _reactStyles.css)(_simpleList2.default.simpleListItemLink, isCurrentItem && _simpleList2.default.modifiers.current, componentClassName),
          onClick: evt => {
            onClick(evt);
            updateCurrentRef(this.ref, this.props);
          },
          ref: this.ref
        }, componentProps, additionalComponentProps), children));
      });
    }

  }

  exports.SimpleListItem = SimpleListItem;

  _defineProperty(SimpleListItem, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    component: _propTypes2.default.oneOf(['button', 'a']),
    componentClassName: _propTypes2.default.string,
    componentProps: _propTypes2.default.any,
    isCurrent: _propTypes2.default.bool,
    onClick: _propTypes2.default.func,
    type: _propTypes2.default.oneOf(['button', 'submit', 'reset']),
    href: _propTypes2.default.string
  });

  _defineProperty(SimpleListItem, "defaultProps", {
    children: null,
    className: '',
    isCurrent: false,
    component: 'button',
    componentClassName: '',
    type: 'button',
    href: '',
    onClick: () => {}
  });
});
//# sourceMappingURL=SimpleListItem.js.map