(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Wizard/wizard", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Wizard/wizard"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.wizard, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _wizard, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.WizardNav = undefined;

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

  const WizardNav = exports.WizardNav = ({
    children,
    ariaLabel,
    isOpen = false,
    returnList = false
  }) => {
    const innerList = React.createElement("ol", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardNavList)
    }, children);

    if (returnList) {
      return innerList;
    }

    return React.createElement("nav", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardNav, isOpen && 'pf-m-expanded'),
      "aria-label": ariaLabel
    }, React.createElement("ol", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardNavList)
    }, children));
  };

  WizardNav.propTypes = {
    children: _propTypes2.default.any,
    ariaLabel: _propTypes2.default.string,
    isOpen: _propTypes2.default.bool,
    returnList: _propTypes2.default.bool
  };
});
//# sourceMappingURL=WizardNav.js.map