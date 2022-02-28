(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/ContextSelector/context-selector", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/search-icon", "./ContextSelectorToggle", "./ContextSelectorMenuList", "./contextSelectorConstants", "../Button", "../TextInput", "../InputGroup", "../../helpers/constants", "../../helpers"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/ContextSelector/context-selector"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/search-icon"), require("./ContextSelectorToggle"), require("./ContextSelectorMenuList"), require("./contextSelectorConstants"), require("../Button"), require("../TextInput"), require("../InputGroup"), require("../../helpers/constants"), require("../../helpers"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.contextSelector, global.reactStyles, global.searchIcon, global.ContextSelectorToggle, global.ContextSelectorMenuList, global.contextSelectorConstants, global.Button, global.TextInput, global.InputGroup, global.constants, global.helpers);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _contextSelector, _reactStyles, _searchIcon, _ContextSelectorToggle, _ContextSelectorMenuList, _contextSelectorConstants, _Button, _TextInput, _InputGroup, _constants, _helpers) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ContextSelector = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _contextSelector2 = _interopRequireDefault(_contextSelector);

  var _searchIcon2 = _interopRequireDefault(_searchIcon);

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

  // seed for the aria-labelledby ID
  let currentId = 0;
  const newId = currentId++;

  class ContextSelector extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "parentRef", React.createRef());

      _defineProperty(this, "onEnterPressed", event => {
        if (event.charCode === _constants.KEY_CODES.ENTER) {
          this.props.onSearchButtonClick();
        }
      });
    }

    render() {
      const toggleId = `pf-context-selector-toggle-id-${newId}`;
      const screenReaderLabelId = `pf-context-selector-label-id-${newId}`;
      const searchButtonId = `pf-context-selector-search-button-id-${newId}`;

      const _this$props = this.props,
            {
        children,
        className,
        isOpen,
        onToggle,
        onSelect,
        screenReaderLabel,
        toggleText,
        searchButtonAriaLabel,
        searchInputValue,
        onSearchInputChange,
        searchInputPlaceholder,
        onSearchButtonClick
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["children", "className", "isOpen", "onToggle", "onSelect", "screenReaderLabel", "toggleText", "searchButtonAriaLabel", "searchInputValue", "onSearchInputChange", "searchInputPlaceholder", "onSearchButtonClick"]);

      return React.createElement("div", _extends({
        className: (0, _reactStyles.css)(_contextSelector2.default.contextSelector, isOpen && _contextSelector2.default.modifiers.expanded, className),
        ref: this.parentRef
      }, props), screenReaderLabel && React.createElement("span", {
        id: screenReaderLabelId,
        hidden: true
      }, screenReaderLabel), React.createElement(_ContextSelectorToggle.ContextSelectorToggle, {
        onToggle: onToggle,
        isOpen: isOpen,
        toggleText: toggleText,
        id: toggleId,
        parentRef: this.parentRef.current,
        "aria-labelledby": `${screenReaderLabelId} ${toggleId}`
      }), isOpen && React.createElement("div", {
        className: (0, _reactStyles.css)(_contextSelector2.default.contextSelectorMenu)
      }, isOpen && React.createElement(_helpers.FocusTrap, {
        focusTrapOptions: {
          clickOutsideDeactivates: true
        }
      }, React.createElement("div", {
        className: (0, _reactStyles.css)(_contextSelector2.default.contextSelectorMenuInput)
      }, React.createElement(_InputGroup.InputGroup, null, React.createElement(_TextInput.TextInput, {
        value: searchInputValue,
        type: "search",
        placeholder: searchInputPlaceholder,
        onChange: onSearchInputChange,
        onKeyPress: this.onEnterPressed,
        "aria-labelledby": searchButtonId
      }), React.createElement(_Button.Button, {
        variant: _Button.ButtonVariant.control,
        "aria-label": searchButtonAriaLabel,
        id: searchButtonId,
        onClick: onSearchButtonClick
      }, React.createElement(_searchIcon2.default, {
        "aria-hidden": "true"
      })))), React.createElement(_contextSelectorConstants.ContextSelectorContext.Provider, {
        value: {
          onSelect
        }
      }, React.createElement(_ContextSelectorMenuList.ContextSelectorMenuList, {
        isOpen: isOpen
      }, children)))));
    }

  }

  exports.ContextSelector = ContextSelector;

  _defineProperty(ContextSelector, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    isOpen: _propTypes2.default.bool,
    onToggle: _propTypes2.default.func,
    onSelect: _propTypes2.default.func,
    screenReaderLabel: _propTypes2.default.string,
    toggleText: _propTypes2.default.string,
    searchButtonAriaLabel: _propTypes2.default.string,
    searchInputValue: _propTypes2.default.string,
    onSearchInputChange: _propTypes2.default.func,
    searchInputPlaceholder: _propTypes2.default.string,
    onSearchButtonClick: _propTypes2.default.func
  });

  _defineProperty(ContextSelector, "defaultProps", {
    children: null,
    className: '',
    isOpen: false,
    onToggle: () => undefined,
    onSelect: () => undefined,
    screenReaderLabel: '',
    toggleText: '',
    searchButtonAriaLabel: 'Search menu items',
    searchInputValue: '',
    onSearchInputChange: () => undefined,
    searchInputPlaceholder: 'Search',
    onSearchButtonClick: () => undefined
  });
});
//# sourceMappingURL=ContextSelector.js.map