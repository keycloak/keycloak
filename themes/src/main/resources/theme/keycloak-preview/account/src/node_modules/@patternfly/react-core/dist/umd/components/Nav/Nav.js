(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Nav/nav", "@patternfly/react-styles", "../withOuia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Nav/nav"), require("@patternfly/react-styles"), require("../withOuia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.nav, global.reactStyles, global.withOuia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _nav, _reactStyles, _withOuia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Nav = exports.NavContext = undefined;

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

  const NavContext = exports.NavContext = React.createContext({});

  class Nav extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "state", {
        showLeftScrollButton: false,
        showRightScrollButton: false
      });

      _defineProperty(this, "updateScrollButtonState", state => {
        const {
          showLeftScrollButton,
          showRightScrollButton
        } = state;
        this.setState({
          showLeftScrollButton,
          showRightScrollButton
        });
      });
    } // Callback from NavItem


    onSelect(event, groupId, itemId, to, preventDefault, onClick) {
      if (preventDefault) {
        event.preventDefault();
      }

      this.props.onSelect({
        groupId,
        itemId,
        event,
        to
      });

      if (onClick) {
        onClick(event, itemId, groupId, to);
      }
    } // Callback from NavExpandable


    onToggle(event, groupId, toggleValue) {
      this.props.onToggle({
        event,
        groupId,
        isExpanded: toggleValue
      });
    }

    render() {
      const _this$props = this.props,
            {
        'aria-label': ariaLabel,
        children,
        className,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onSelect,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onToggle,
        theme,
        ouiaContext,
        ouiaId
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["aria-label", "children", "className", "onSelect", "onToggle", "theme", "ouiaContext", "ouiaId"]);

      const {
        showLeftScrollButton,
        showRightScrollButton
      } = this.state;
      const childrenProps = children.props;
      return React.createElement(NavContext.Provider, {
        value: {
          onSelect: (event, groupId, itemId, to, preventDefault, onClick) => this.onSelect(event, groupId, itemId, to, preventDefault, onClick),
          onToggle: (event, groupId, expanded) => this.onToggle(event, groupId, expanded),
          updateScrollButtonState: this.updateScrollButtonState
        }
      }, React.createElement("nav", _extends({
        className: (0, _reactStyles.css)(_nav2.default.nav, theme === 'dark' && _nav2.default.modifiers.dark, showLeftScrollButton && _nav2.default.modifiers.start, showRightScrollButton && _nav2.default.modifiers.end, className),
        "aria-label": ariaLabel === '' ? typeof childrenProps !== 'undefined' && childrenProps.variant === 'tertiary' ? 'Local' : 'Global' : ariaLabel
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Nav',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }, props), children));
    }

  }

  _defineProperty(Nav, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    onSelect: _propTypes2.default.func,
    onToggle: _propTypes2.default.func,
    'aria-label': _propTypes2.default.string,
    theme: _propTypes2.default.oneOf(['dark', 'light'])
  });

  _defineProperty(Nav, "defaultProps", {
    'aria-label': '',
    children: null,
    className: '',
    onSelect: () => undefined,
    onToggle: () => undefined,
    theme: 'light'
  });

  const NavWithOuiaContext = (0, _withOuia.withOuiaContext)(Nav);
  exports.Nav = NavWithOuiaContext;
});
//# sourceMappingURL=Nav.js.map