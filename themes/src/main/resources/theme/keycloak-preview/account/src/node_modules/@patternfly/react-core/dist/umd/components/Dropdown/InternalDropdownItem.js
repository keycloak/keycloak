(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "./dropdownConstants", "../../helpers/constants", "../Tooltip", "@patternfly/react-styles/css/components/Dropdown/dropdown"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("./dropdownConstants"), require("../../helpers/constants"), require("../Tooltip"), require("@patternfly/react-styles/css/components/Dropdown/dropdown"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.dropdownConstants, global.constants, global.Tooltip, global.dropdown);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _dropdownConstants, _constants, _Tooltip, _dropdown) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.InternalDropdownItem = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

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

  class InternalDropdownItem extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "ref", React.createRef());

      _defineProperty(this, "additionalRef", React.createRef());

      _defineProperty(this, "getInnerNode", node => node && node.childNodes && node.childNodes.length ? node.childNodes[0] : node);

      _defineProperty(this, "onKeyDown", event => {
        // Detected key press on this item, notify the menu parent so that the appropriate item can be focused
        const innerIndex = event.target === this.ref.current ? 0 : 1;

        if (!this.props.customChild) {
          event.preventDefault();
        }

        if (event.key === 'ArrowUp') {
          this.props.context.keyHandler(this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.UP);
        } else if (event.key === 'ArrowDown') {
          this.props.context.keyHandler(this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.DOWN);
        } else if (event.key === 'ArrowRight') {
          this.props.context.keyHandler(this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.RIGHT);
        } else if (event.key === 'ArrowLeft') {
          this.props.context.keyHandler(this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.LEFT);
        } else if (event.key === 'Enter' || event.key === ' ') {
          event.target.click();
          this.props.enterTriggersArrowDown && this.props.context.keyHandler(this.props.index, innerIndex, _constants.KEYHANDLER_DIRECTION.DOWN);
        }
      });
    }

    componentDidMount() {
      const {
        context,
        index,
        isDisabled,
        role,
        customChild
      } = this.props;
      const customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
      context.sendRef(index, [customRef, customChild ? customRef : this.additionalRef.current], isDisabled, role === 'separator');
    }

    componentDidUpdate() {
      const {
        context,
        index,
        isDisabled,
        role,
        customChild
      } = this.props;
      const customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
      context.sendRef(index, [customRef, customChild ? customRef : this.additionalRef.current], isDisabled, role === 'separator');
    }

    extendAdditionalChildRef() {
      const {
        additionalChild
      } = this.props;
      return React.cloneElement(additionalChild, {
        ref: this.additionalRef
      });
    }

    render() {
      /* eslint-disable @typescript-eslint/no-unused-vars */
      const _this$props = this.props,
            {
        className,
        children,
        isHovered,
        context,
        onClick,
        component,
        variant,
        role,
        isDisabled,
        index,
        href,
        tooltip,
        tooltipProps,
        id,
        componentID,
        listItemClassName,
        additionalChild,
        customChild,
        enterTriggersArrowDown
      } = _this$props,
            additionalProps = _objectWithoutProperties(_this$props, ["className", "children", "isHovered", "context", "onClick", "component", "variant", "role", "isDisabled", "index", "href", "tooltip", "tooltipProps", "id", "componentID", "listItemClassName", "additionalChild", "customChild", "enterTriggersArrowDown"]);
      /* eslint-enable @typescript-eslint/no-unused-vars */


      const Component = component;
      let classes;

      if (Component === 'a') {
        additionalProps['aria-disabled'] = isDisabled;
        additionalProps.tabIndex = isDisabled ? -1 : additionalProps.tabIndex;
      } else if (Component === 'button') {
        additionalProps.disabled = isDisabled;
        additionalProps.type = additionalProps.type || 'button';
      }

      const renderWithTooltip = childNode => tooltip ? React.createElement(_Tooltip.Tooltip, _extends({
        content: tooltip
      }, tooltipProps), childNode) : childNode;

      return React.createElement(_dropdownConstants.DropdownContext.Consumer, null, ({
        onSelect,
        itemClass,
        disabledClass,
        hoverClass
      }) => {
        if (this.props.role === 'separator') {
          classes = (0, _reactStyles.css)(variant === 'icon' && _dropdown2.default.modifiers.icon, className);
        } else {
          classes = (0, _reactStyles.css)(variant === 'icon' && _dropdown2.default.modifiers.icon, className, isDisabled && disabledClass, isHovered && hoverClass, itemClass);
        }

        if (customChild) {
          return React.cloneElement(customChild, {
            ref: this.ref,
            onKeyDown: this.onKeyDown
          });
        }

        return React.createElement("li", {
          className: listItemClassName || null,
          role: role,
          onKeyDown: this.onKeyDown,
          onClick: event => {
            if (!isDisabled) {
              onClick(event);
              onSelect(event);
            }
          },
          id: id
        }, renderWithTooltip(React.isValidElement(component) ? React.cloneElement(component, _objectSpread({
          href,
          id: componentID,
          className: classes
        }, additionalProps)) : React.createElement(Component, _extends({}, additionalProps, {
          href: href,
          ref: this.ref,
          className: classes,
          id: componentID
        }), children)), additionalChild && this.extendAdditionalChildRef());
      });
    }

  }

  exports.InternalDropdownItem = InternalDropdownItem;

  _defineProperty(InternalDropdownItem, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    listItemClassName: _propTypes2.default.string,
    component: _propTypes2.default.node,
    variant: _propTypes2.default.oneOf(['item', 'icon']),
    role: _propTypes2.default.string,
    isDisabled: _propTypes2.default.bool,
    isHovered: _propTypes2.default.bool,
    href: _propTypes2.default.string,
    tooltip: _propTypes2.default.node,
    tooltipProps: _propTypes2.default.any,
    index: _propTypes2.default.number,
    context: _propTypes2.default.shape({
      keyHandler: _propTypes2.default.func,
      sendRef: _propTypes2.default.func
    }),
    onClick: _propTypes2.default.func,
    id: _propTypes2.default.string,
    componentID: _propTypes2.default.string,
    additionalChild: _propTypes2.default.node,
    customChild: _propTypes2.default.node,
    enterTriggersArrowDown: _propTypes2.default.bool
  });

  _defineProperty(InternalDropdownItem, "defaultProps", {
    className: '',
    isHovered: false,
    component: 'a',
    variant: 'item',
    role: 'none',
    isDisabled: false,
    tooltipProps: {},
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: event => undefined,
    index: -1,
    context: {
      keyHandler: () => {},
      sendRef: () => {}
    },
    enterTriggersArrowDown: false
  });
});
//# sourceMappingURL=InternalDropdownItem.js.map