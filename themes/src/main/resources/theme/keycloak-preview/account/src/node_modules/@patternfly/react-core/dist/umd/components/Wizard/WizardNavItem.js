(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Wizard/wizard"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Wizard/wizard"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.wizard);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _wizard) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.WizardNavItem = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _wizard2 = _interopRequireDefault(_wizard);

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

  const WizardNavItem = exports.WizardNavItem = ({
    children = null,
    text = '',
    isCurrent = false,
    isDisabled = false,
    step,
    onNavItemClick = () => undefined,
    navItemComponent = 'a'
  }) => {
    const NavItemComponent = navItemComponent;
    return React.createElement("li", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardNavItem)
    }, React.createElement(NavItemComponent, {
      "aria-current": isCurrent && !children ? 'page' : false,
      onClick: () => onNavItemClick(step),
      className: (0, _reactStyles.css)(_wizard2.default.wizardNavLink, isCurrent && 'pf-m-current', isDisabled && 'pf-m-disabled'),
      "aria-disabled": isDisabled ? true : false,
      tabIndex: isDisabled ? -1 : undefined
    }, text), children);
  };

  WizardNavItem.propTypes = {
    children: _propTypes2.default.node,
    text: _propTypes2.default.string,
    isCurrent: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    step: _propTypes2.default.number.isRequired,
    onNavItemClick: _propTypes2.default.func,
    navItemComponent: _propTypes2.default.node
  };
});
//# sourceMappingURL=WizardNavItem.js.map