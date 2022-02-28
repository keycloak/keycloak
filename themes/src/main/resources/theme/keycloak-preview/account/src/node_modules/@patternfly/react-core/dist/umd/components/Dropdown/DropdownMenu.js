(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "react-dom", "@patternfly/react-styles/css/components/Dropdown/dropdown", "@patternfly/react-styles", "../../helpers/util", "./dropdownConstants"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("react-dom"), require("@patternfly/react-styles/css/components/Dropdown/dropdown"), require("@patternfly/react-styles"), require("../../helpers/util"), require("./dropdownConstants"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactDom, global.dropdown, global.reactStyles, global.util, global.dropdownConstants);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactDom, _dropdown, _reactStyles, _util, _dropdownConstants) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DropdownMenu = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var ReactDOM = _interopRequireWildcard(_reactDom);

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

  class DropdownMenu extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "refsCollection", []);

      _defineProperty(this, "childKeyHandler", (index, innerIndex, position, custom = false) => {
        (0, _util.keyHandler)(index, innerIndex, position, this.refsCollection, this.props.isGrouped ? this.refsCollection : React.Children.toArray(this.props.children), custom);
      });

      _defineProperty(this, "sendRef", (index, nodes, isDisabled, isSeparator) => {
        this.refsCollection[index] = [];
        nodes.map((node, innerIndex) => {
          if (!node) {
            this.refsCollection[index][innerIndex] = null;
          } else if (!node.getAttribute) {
            // eslint-disable-next-line react/no-find-dom-node
            this.refsCollection[index][innerIndex] = ReactDOM.findDOMNode(node);
          } else if (isDisabled || isSeparator) {
            this.refsCollection[index][innerIndex] = null;
          } else {
            this.refsCollection[index][innerIndex] = node;
          }
        });
      });
    }

    componentDidMount() {
      const {
        autoFocus
      } = this.props;

      if (autoFocus) {
        // Focus first non-disabled element
        const focusTargetCollection = this.refsCollection.find(ref => ref && ref[0] && !ref[0].hasAttribute('disabled'));
        const focusTarget = focusTargetCollection && focusTargetCollection[0];

        if (focusTarget && focusTarget.focus) {
          focusTarget.focus();
        }
      }
    }

    shouldComponentUpdate() {
      // reset refsCollection before updating to account for child removal between mounts
      this.refsCollection = [];
      return true;
    }

    extendChildren() {
      const {
        children,
        isGrouped
      } = this.props;

      if (isGrouped) {
        let index = 0;
        return React.Children.map(children, groupedChildren => {
          const group = groupedChildren;
          return React.cloneElement(group, _objectSpread({}, group.props && group.props.children && {
            children: group.props.children.constructor === Array && React.Children.map(group.props.children, option => React.cloneElement(option, {
              index: index++
            })) || React.cloneElement(group.props.children, {
              index: index++
            })
          }));
        });
      }

      return React.Children.map(children, (child, index) => React.cloneElement(child, {
        index
      }));
    }

    render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const _this$props = this.props,
            {
        className,
        isOpen,
        position,
        children,
        component,
        isGrouped,
        openedOnEnter
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["className", "isOpen", "position", "children", "component", "isGrouped", "openedOnEnter"]);

      return React.createElement(_dropdownConstants.DropdownArrowContext.Provider, {
        value: {
          keyHandler: this.childKeyHandler,
          sendRef: this.sendRef
        }
      }, component === 'div' ? React.createElement(_dropdownConstants.DropdownContext.Consumer, null, ({
        onSelect,
        menuClass
      }) => React.createElement("div", {
        className: (0, _reactStyles.css)(menuClass, position === _dropdownConstants.DropdownPosition.right && _dropdown2.default.modifiers.alignRight, className),
        hidden: !isOpen,
        onClick: event => onSelect && onSelect(event)
      }, children)) : isGrouped && React.createElement(_dropdownConstants.DropdownContext.Consumer, null, ({
        menuClass,
        menuComponent
      }) => {
        const MenuComponent = menuComponent || 'div';
        return React.createElement(MenuComponent, _extends({}, props, {
          className: (0, _reactStyles.css)(menuClass, position === _dropdownConstants.DropdownPosition.right && _dropdown2.default.modifiers.alignRight, className),
          hidden: !isOpen,
          role: "menu"
        }), this.extendChildren());
      }) || React.createElement(_dropdownConstants.DropdownContext.Consumer, null, ({
        menuClass,
        menuComponent
      }) => {
        const MenuComponent = menuComponent || component;
        return React.createElement(MenuComponent, _extends({}, props, {
          className: (0, _reactStyles.css)(menuClass, position === _dropdownConstants.DropdownPosition.right && _dropdown2.default.modifiers.alignRight, className),
          hidden: !isOpen,
          role: "menu"
        }), this.extendChildren());
      }));
    }

  }

  exports.DropdownMenu = DropdownMenu;

  _defineProperty(DropdownMenu, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    isOpen: _propTypes2.default.bool,
    openedOnEnter: _propTypes2.default.bool,
    autoFocus: _propTypes2.default.bool,
    component: _propTypes2.default.node,
    position: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf(['right']), _propTypes2.default.oneOf(['left'])]),
    isGrouped: _propTypes2.default.bool
  });

  _defineProperty(DropdownMenu, "defaultProps", {
    className: '',
    isOpen: true,
    openedOnEnter: false,
    autoFocus: true,
    position: _dropdownConstants.DropdownPosition.left,
    component: 'ul',
    isGrouped: false
  });
});
//# sourceMappingURL=DropdownMenu.js.map