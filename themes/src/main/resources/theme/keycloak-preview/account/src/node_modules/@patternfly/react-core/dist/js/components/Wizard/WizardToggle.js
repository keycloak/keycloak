"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.WizardToggle = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _wizard = _interopRequireDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));

var _angleRightIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-right-icon"));

var _caretDownIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/caret-down-icon"));

var _WizardBody = require("./WizardBody");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var WizardToggle = function WizardToggle(_ref) {
  var isNavOpen = _ref.isNavOpen,
      onNavToggle = _ref.onNavToggle,
      nav = _ref.nav,
      steps = _ref.steps,
      activeStep = _ref.activeStep,
      children = _ref.children,
      _ref$hasBodyPadding = _ref.hasBodyPadding,
      hasBodyPadding = _ref$hasBodyPadding === void 0 ? true : _ref$hasBodyPadding;
  var activeStepIndex;
  var activeStepName;
  var activeStepSubName;

  for (var i = 0; i < steps.length; i++) {
    if (activeStep.id && steps[i].id === activeStep.id || steps[i].name === activeStep.name) {
      activeStepIndex = i + 1;
      activeStepName = steps[i].name;
      break;
    } else if (steps[i].steps) {
      var _iteratorNormalCompletion = true;
      var _didIteratorError = false;
      var _iteratorError = undefined;

      try {
        for (var _iterator = steps[i].steps[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          var step = _step.value;

          if (activeStep.id && step.id === activeStep.id || step.name === activeStep.name) {
            activeStepIndex = i + 1;
            activeStepName = steps[i].name;
            activeStepSubName = step.name;
            break;
          }
        }
      } catch (err) {
        _didIteratorError = true;
        _iteratorError = err;
      } finally {
        try {
          if (!_iteratorNormalCompletion && _iterator["return"] != null) {
            _iterator["return"]();
          }
        } finally {
          if (_didIteratorError) {
            throw _iteratorError;
          }
        }
      }
    }
  }

  return React.createElement(React.Fragment, null, React.createElement("button", {
    onClick: function onClick() {
      return onNavToggle(!isNavOpen);
    },
    className: (0, _reactStyles.css)(_wizard["default"].wizardToggle, isNavOpen && 'pf-m-expanded'),
    "aria-expanded": isNavOpen
  }, React.createElement("ol", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardToggleList)
  }, React.createElement("li", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardToggleListItem)
  }, React.createElement("span", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardToggleNum)
  }, activeStepIndex), " ", activeStepName, activeStepSubName && React.createElement(_angleRightIcon["default"], {
    className: (0, _reactStyles.css)(_wizard["default"].wizardToggleSeparator),
    "aria-hidden": "true"
  })), activeStepSubName && React.createElement("li", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardToggleListItem)
  }, activeStepSubName)), React.createElement(_caretDownIcon["default"], {
    className: (0, _reactStyles.css)(_wizard["default"].wizardToggleIcon),
    "aria-hidden": "true"
  })), React.createElement("div", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardOuterWrap)
  }, React.createElement("div", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardInnerWrap)
  }, nav(isNavOpen), React.createElement(_WizardBody.WizardBody, {
    hasBodyPadding: hasBodyPadding
  }, activeStep.component)), children));
};

exports.WizardToggle = WizardToggle;
WizardToggle.propTypes = {
  nav: _propTypes["default"].func.isRequired,
  steps: _propTypes["default"].arrayOf(_propTypes["default"].any).isRequired,
  activeStep: _propTypes["default"].any.isRequired,
  children: _propTypes["default"].node.isRequired,
  hasBodyPadding: _propTypes["default"].bool.isRequired,
  isNavOpen: _propTypes["default"].bool.isRequired,
  onNavToggle: _propTypes["default"].func.isRequired
};
//# sourceMappingURL=WizardToggle.js.map