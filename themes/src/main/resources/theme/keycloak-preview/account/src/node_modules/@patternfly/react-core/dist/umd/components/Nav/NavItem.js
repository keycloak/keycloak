(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Nav/nav", "@patternfly/react-styles", "./Nav"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Nav/nav"), require("@patternfly/react-styles"), require("./Nav"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.nav, global.reactStyles, global.Nav);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _nav, _reactStyles, _Nav) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.NavItem = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _nav2 = _interopRequireDefault(_nav);

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

  const NavItem = exports.NavItem = _ref => {
    let {
      children = null,
      className = '',
      to = '',
      isActive = false,
      groupId = null,
      itemId = null,
      preventDefault = false,
      onClick = null,
      component = 'a'
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "className", "to", "isActive", "groupId", "itemId", "preventDefault", "onClick", "component"]);

    const Component = component;

    const renderDefaultLink = () => {
      const preventLinkDefault = preventDefault || !to;
      return React.createElement(_Nav.NavContext.Consumer, null, context => React.createElement(Component, _extends({
        href: to,
        onClick: e => context.onSelect(e, groupId, itemId, to, preventLinkDefault, onClick),
        className: (0, _reactStyles.css)(_nav2.default.navLink, isActive && _nav2.default.modifiers.current, className),
        "aria-current": isActive ? 'page' : null
      }, props), children));
    };

    const renderClonedChild = child => React.createElement(_Nav.NavContext.Consumer, null, context => React.cloneElement(child, {
      onClick: e => context.onSelect(e, groupId, itemId, to, preventDefault, onClick),
      className: (0, _reactStyles.css)(_nav2.default.navLink, isActive && _nav2.default.modifiers.current, child.props && child.props.className),
      'aria-current': isActive ? 'page' : null
    }));

    return React.createElement("li", {
      className: (0, _reactStyles.css)(_nav2.default.navItem, className)
    }, React.isValidElement(children) ? renderClonedChild(children) : renderDefaultLink());
  };

  NavItem.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    to: _propTypes2.default.string,
    isActive: _propTypes2.default.bool,
    groupId: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number, _propTypes2.default.oneOf([null])]),
    itemId: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number, _propTypes2.default.oneOf([null])]),
    preventDefault: _propTypes2.default.bool,
    onClick: _propTypes2.default.func,
    component: _propTypes2.default.node
  };
});
//# sourceMappingURL=NavItem.js.map