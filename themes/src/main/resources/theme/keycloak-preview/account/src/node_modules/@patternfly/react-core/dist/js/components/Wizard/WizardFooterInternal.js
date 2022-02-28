"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.WizardFooterInternal = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _wizard = _interopRequireDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));

var _Button = require("../Button");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var WizardFooterInternal = function WizardFooterInternal(_ref) {
  var onNext = _ref.onNext,
      onBack = _ref.onBack,
      onClose = _ref.onClose,
      isValid = _ref.isValid,
      firstStep = _ref.firstStep,
      activeStep = _ref.activeStep,
      nextButtonText = _ref.nextButtonText,
      backButtonText = _ref.backButtonText,
      cancelButtonText = _ref.cancelButtonText;
  return React.createElement("footer", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardFooter)
  }, React.createElement(_Button.Button, {
    variant: _Button.ButtonVariant.primary,
    type: "submit",
    onClick: onNext,
    isDisabled: !isValid
  }, nextButtonText), !activeStep.hideBackButton && React.createElement(_Button.Button, {
    variant: _Button.ButtonVariant.secondary,
    onClick: onBack,
    className: (0, _reactStyles.css)(firstStep && 'pf-m-disabled')
  }, backButtonText), !activeStep.hideCancelButton && React.createElement(_Button.Button, {
    variant: _Button.ButtonVariant.link,
    onClick: onClose
  }, cancelButtonText));
};

exports.WizardFooterInternal = WizardFooterInternal;
WizardFooterInternal.propTypes = {
  onNext: _propTypes["default"].any.isRequired,
  onBack: _propTypes["default"].any.isRequired,
  onClose: _propTypes["default"].any.isRequired,
  isValid: _propTypes["default"].bool.isRequired,
  firstStep: _propTypes["default"].bool.isRequired,
  activeStep: _propTypes["default"].any.isRequired,
  nextButtonText: _propTypes["default"].string.isRequired,
  backButtonText: _propTypes["default"].string.isRequired,
  cancelButtonText: _propTypes["default"].string.isRequired
};
//# sourceMappingURL=WizardFooterInternal.js.map