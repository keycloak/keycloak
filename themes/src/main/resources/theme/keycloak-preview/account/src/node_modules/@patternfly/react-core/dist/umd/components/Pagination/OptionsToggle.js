(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/OptionsMenu/options-menu", "@patternfly/react-styles", "../../helpers", "../Dropdown"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"), require("@patternfly/react-styles"), require("../../helpers"), require("../Dropdown"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.optionsMenu, global.reactStyles, global.helpers, global.Dropdown);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _optionsMenu, _reactStyles, _helpers, _Dropdown) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OptionsToggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _optionsMenu2 = _interopRequireDefault(_optionsMenu);

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

  let toggleId = 0;

  const OptionsToggle = exports.OptionsToggle = ({
    itemsTitle = 'items',
    optionsToggle = 'Select',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    itemsPerPageTitle = 'Items per page',
    firstIndex = 0,
    lastIndex = 0,
    itemCount = 0,
    widgetId = '',
    showToggle = true,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onToggle = _isOpen => undefined,
    isOpen = false,
    isDisabled = false,
    parentRef = null,
    toggleTemplate: ToggleTemplate = '',
    onEnter = null
  }) => React.createElement("div", {
    className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuToggle, isDisabled && _optionsMenu2.default.modifiers.disabled, _optionsMenu2.default.modifiers.plain, _optionsMenu2.default.modifiers.text)
  }, showToggle && React.createElement(React.Fragment, null, React.createElement("span", {
    className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuToggleText)
  }, typeof ToggleTemplate === 'string' ? (0, _helpers.fillTemplate)(ToggleTemplate, {
    firstIndex,
    lastIndex,
    itemCount,
    itemsTitle
  }) : React.createElement(ToggleTemplate, {
    firstIndex: firstIndex,
    lastIndex: lastIndex,
    itemCount: itemCount,
    itemsTitle: itemsTitle
  })), React.createElement(_Dropdown.DropdownToggle, {
    onEnter: onEnter,
    "aria-label": optionsToggle,
    onToggle: onToggle,
    isDisabled: isDisabled || itemCount <= 0,
    isOpen: isOpen,
    id: `${widgetId}-toggle-${toggleId++}`,
    className: _optionsMenu2.default.optionsMenuToggleButton,
    parentRef: parentRef
  })));

  OptionsToggle.propTypes = {
    itemsTitle: _propTypes2.default.string,
    optionsToggle: _propTypes2.default.string,
    itemsPerPageTitle: _propTypes2.default.string,
    firstIndex: _propTypes2.default.number,
    lastIndex: _propTypes2.default.number,
    itemCount: _propTypes2.default.number,
    widgetId: _propTypes2.default.string,
    showToggle: _propTypes2.default.bool,
    onToggle: _propTypes2.default.func,
    isOpen: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    parentRef: _propTypes2.default.any,
    toggleTemplate: _propTypes2.default.oneOfType([_propTypes2.default.func, _propTypes2.default.string]),
    onEnter: _propTypes2.default.func
  };
});
//# sourceMappingURL=OptionsToggle.js.map