(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "react", "@patternfly/react-styles/css/components/Dropdown/dropdown", "@patternfly/react-styles", "./DropdownMenu", "./dropdownConstants", "../withOuia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("react"), require("@patternfly/react-styles/css/components/Dropdown/dropdown"), require("@patternfly/react-styles"), require("./DropdownMenu"), require("./dropdownConstants"), require("../withOuia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.react, global.dropdown, global.reactStyles, global.DropdownMenu, global.dropdownConstants, global.withOuia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _react, _dropdown, _reactStyles, _DropdownMenu, _dropdownConstants, _withOuia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DropdownWithContext = undefined;

  var React = _interopRequireWildcard(_react);

  var _dropdown2 = _interopRequireDefault(_dropdown);

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

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

  class DropdownWithContext extends React.Component {
    // seed for the aria-labelledby ID
    constructor(props) {
      super(props);

      _defineProperty(this, "openedOnEnter", false);

      _defineProperty(this, "baseComponentRef", React.createRef());

      _defineProperty(this, "onEnter", () => {
        this.openedOnEnter = true;
      });

      if (props.dropdownItems && props.dropdownItems.length > 0 && props.children) {
        // eslint-disable-next-line no-console
        console.error('Children and dropdownItems props have been provided. Only the dropdownItems prop items will be rendered');
      }
    }

    componentDidUpdate() {
      if (!this.props.isOpen) {
        this.openedOnEnter = false;
      }
    }

    render() {
      const _this$props = this.props,
            {
        children,
        className,
        direction,
        dropdownItems,
        isOpen,
        isPlain,
        isGrouped,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onSelect,
        position,
        toggle,
        autoFocus,
        ouiaContext,
        ouiaId,
        ouiaComponentType
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["children", "className", "direction", "dropdownItems", "isOpen", "isPlain", "isGrouped", "onSelect", "position", "toggle", "autoFocus", "ouiaContext", "ouiaId", "ouiaComponentType"]);

      const id = toggle.props.id || `pf-toggle-id-${DropdownWithContext.currentId++}`;
      let component;
      let renderedContent;
      let ariaHasPopup = false;

      if (dropdownItems && dropdownItems.length > 0) {
        component = 'ul';
        renderedContent = dropdownItems;
        ariaHasPopup = true;
      } else {
        component = 'div';
        renderedContent = React.Children.toArray(children);
      }

      const openedOnEnter = this.openedOnEnter;
      return React.createElement(_dropdownConstants.DropdownContext.Consumer, null, ({
        baseClass,
        baseComponent,
        id: contextId
      }) => {
        const BaseComponent = baseComponent;
        return React.createElement(BaseComponent, _extends({}, props, {
          className: (0, _reactStyles.css)(baseClass, direction === _dropdownConstants.DropdownDirection.up && _dropdown2.default.modifiers.top, position === _dropdownConstants.DropdownPosition.right && _dropdown2.default.modifiers.alignRight, isOpen && _dropdown2.default.modifiers.expanded, className),
          ref: this.baseComponentRef
        }, ouiaContext.isOuia && {
          'data-ouia-component-type': ouiaComponentType,
          'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
        }), React.Children.map(toggle, oneToggle => React.cloneElement(oneToggle, {
          parentRef: this.baseComponentRef,
          isOpen,
          id,
          isPlain,
          ariaHasPopup,
          onEnter: () => this.onEnter()
        })), isOpen && React.createElement(_DropdownMenu.DropdownMenu, {
          component: component,
          isOpen: isOpen,
          position: position,
          "aria-labelledby": contextId ? `${contextId}-toggle` : id,
          openedOnEnter: openedOnEnter,
          isGrouped: isGrouped,
          autoFocus: openedOnEnter && autoFocus
        }, renderedContent));
      });
    }

  }

  _defineProperty(DropdownWithContext, "currentId", 0);

  _defineProperty(DropdownWithContext, "defaultProps", {
    className: '',
    dropdownItems: [],
    isOpen: false,
    isPlain: false,
    isGrouped: false,
    position: _dropdownConstants.DropdownPosition.left,
    direction: _dropdownConstants.DropdownDirection.down,
    onSelect: () => undefined,
    autoFocus: true,
    ouiaComponentType: 'Dropdown'
  });

  const DropdownWithOuiaContext = (0, _withOuia.withOuiaContext)(DropdownWithContext);
  exports.DropdownWithContext = DropdownWithOuiaContext;
});
//# sourceMappingURL=DropdownWithContext.js.map