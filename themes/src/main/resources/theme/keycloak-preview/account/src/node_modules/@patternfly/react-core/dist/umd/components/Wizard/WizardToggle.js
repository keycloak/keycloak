(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Wizard/wizard", "@patternfly/react-icons/dist/js/icons/angle-right-icon", "@patternfly/react-icons/dist/js/icons/caret-down-icon", "./WizardBody"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Wizard/wizard"), require("@patternfly/react-icons/dist/js/icons/angle-right-icon"), require("@patternfly/react-icons/dist/js/icons/caret-down-icon"), require("./WizardBody"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.wizard, global.angleRightIcon, global.caretDownIcon, global.WizardBody);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _wizard, _angleRightIcon, _caretDownIcon, _WizardBody) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.WizardToggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _wizard2 = _interopRequireDefault(_wizard);

  var _angleRightIcon2 = _interopRequireDefault(_angleRightIcon);

  var _caretDownIcon2 = _interopRequireDefault(_caretDownIcon);

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

  const WizardToggle = exports.WizardToggle = ({
    isNavOpen,
    onNavToggle,
    nav,
    steps,
    activeStep,
    children,
    hasBodyPadding = true
  }) => {
    let activeStepIndex;
    let activeStepName;
    let activeStepSubName;

    for (let i = 0; i < steps.length; i++) {
      if (activeStep.id && steps[i].id === activeStep.id || steps[i].name === activeStep.name) {
        activeStepIndex = i + 1;
        activeStepName = steps[i].name;
        break;
      } else if (steps[i].steps) {
        for (const step of steps[i].steps) {
          if (activeStep.id && step.id === activeStep.id || step.name === activeStep.name) {
            activeStepIndex = i + 1;
            activeStepName = steps[i].name;
            activeStepSubName = step.name;
            break;
          }
        }
      }
    }

    return React.createElement(React.Fragment, null, React.createElement("button", {
      onClick: () => onNavToggle(!isNavOpen),
      className: (0, _reactStyles.css)(_wizard2.default.wizardToggle, isNavOpen && 'pf-m-expanded'),
      "aria-expanded": isNavOpen
    }, React.createElement("ol", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardToggleList)
    }, React.createElement("li", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardToggleListItem)
    }, React.createElement("span", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardToggleNum)
    }, activeStepIndex), " ", activeStepName, activeStepSubName && React.createElement(_angleRightIcon2.default, {
      className: (0, _reactStyles.css)(_wizard2.default.wizardToggleSeparator),
      "aria-hidden": "true"
    })), activeStepSubName && React.createElement("li", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardToggleListItem)
    }, activeStepSubName)), React.createElement(_caretDownIcon2.default, {
      className: (0, _reactStyles.css)(_wizard2.default.wizardToggleIcon),
      "aria-hidden": "true"
    })), React.createElement("div", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardOuterWrap)
    }, React.createElement("div", {
      className: (0, _reactStyles.css)(_wizard2.default.wizardInnerWrap)
    }, nav(isNavOpen), React.createElement(_WizardBody.WizardBody, {
      hasBodyPadding: hasBodyPadding
    }, activeStep.component)), children));
  };

  WizardToggle.propTypes = {
    nav: _propTypes2.default.func.isRequired,
    steps: _propTypes2.default.arrayOf(_propTypes2.default.any).isRequired,
    activeStep: _propTypes2.default.any.isRequired,
    children: _propTypes2.default.node.isRequired,
    hasBodyPadding: _propTypes2.default.bool.isRequired,
    isNavOpen: _propTypes2.default.bool.isRequired,
    onNavToggle: _propTypes2.default.func.isRequired
  };
});
//# sourceMappingURL=WizardToggle.js.map