(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Nav/nav", "@patternfly/react-styles/css/utilities/Accessibility/accessibility", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/angle-right-icon", "../../helpers/util", "./Nav"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Nav/nav"), require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/angle-right-icon"), require("../../helpers/util"), require("./Nav"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.nav, global.accessibility, global.reactStyles, global.angleRightIcon, global.util, global.Nav);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _nav, _accessibility, _reactStyles, _angleRightIcon, _util, _Nav) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.NavExpandable = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _nav2 = _interopRequireDefault(_nav);

  var _accessibility2 = _interopRequireDefault(_accessibility);

  var _angleRightIcon2 = _interopRequireDefault(_angleRightIcon);

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

  class NavExpandable extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "id", this.props.id || (0, _util.getUniqueId)());

      _defineProperty(this, "state", {
        expandedState: this.props.isExpanded
      });

      _defineProperty(this, "onExpand", (e, val) => {
        if (this.props.onExpand) {
          this.props.onExpand(e, val);
        } else {
          this.setState({
            expandedState: val
          });
        }
      });

      _defineProperty(this, "handleToggle", (e, onToggle) => {
        // Item events can bubble up, ignore those
        if (e.target.getAttribute('data-component') !== 'pf-nav-expandable') {
          return;
        }

        const {
          groupId
        } = this.props;
        const {
          expandedState
        } = this.state;
        onToggle(e, groupId, !expandedState);
        this.onExpand(e, !expandedState);
      });
    }

    componentDidMount() {
      this.setState({
        expandedState: this.props.isExpanded
      });
    }

    componentDidUpdate(prevProps) {
      if (this.props.isExpanded !== prevProps.isExpanded) {
        this.setState({
          expandedState: this.props.isExpanded
        });
      }
    }

    render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const _this$props = this.props,
            {
        id,
        title,
        srText,
        children,
        className,
        isActive,
        groupId,
        isExpanded,
        onExpand
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["id", "title", "srText", "children", "className", "isActive", "groupId", "isExpanded", "onExpand"]);

      const {
        expandedState
      } = this.state;
      return React.createElement(_Nav.NavContext.Consumer, null, context => React.createElement("li", _extends({
        className: (0, _reactStyles.css)(_nav2.default.navItem, expandedState && _nav2.default.modifiers.expanded, isActive && _nav2.default.modifiers.current, className),
        onClick: e => this.handleToggle(e, context.onToggle)
      }, props), React.createElement("a", {
        "data-component": "pf-nav-expandable",
        className: (0, _reactStyles.css)(_nav2.default.navLink),
        id: srText ? null : this.id,
        href: "#",
        onClick: e => e.preventDefault(),
        onMouseDown: e => e.preventDefault(),
        "aria-expanded": expandedState
      }, title, React.createElement("span", {
        className: (0, _reactStyles.css)(_nav2.default.navToggle)
      }, React.createElement(_angleRightIcon2.default, {
        "aria-hidden": "true"
      }))), React.createElement("section", {
        className: (0, _reactStyles.css)(_nav2.default.navSubnav),
        "aria-labelledby": this.id,
        hidden: expandedState ? null : true
      }, srText && React.createElement("h2", {
        className: (0, _reactStyles.css)(_accessibility2.default.screenReader),
        id: this.id
      }, srText), React.createElement("ul", {
        className: (0, _reactStyles.css)(_nav2.default.navSimpleList)
      }, children))));
    }

  }

  exports.NavExpandable = NavExpandable;

  _defineProperty(NavExpandable, "propTypes", {
    title: _propTypes2.default.string.isRequired,
    srText: _propTypes2.default.string,
    isExpanded: _propTypes2.default.bool,
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    groupId: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number]),
    isActive: _propTypes2.default.bool,
    id: _propTypes2.default.string,
    onExpand: _propTypes2.default.func
  });

  _defineProperty(NavExpandable, "defaultProps", {
    srText: '',
    isExpanded: false,
    children: '',
    className: '',
    groupId: null,
    isActive: false,
    id: ''
  });
});
//# sourceMappingURL=NavExpandable.js.map