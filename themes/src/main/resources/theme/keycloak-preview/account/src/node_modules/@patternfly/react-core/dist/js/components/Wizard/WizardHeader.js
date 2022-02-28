"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.WizardHeader = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _wizard = _interopRequireDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));

var _reactStyles = require("@patternfly/react-styles");

var _Button = require("../Button");

var _Title = require("../Title");

var _timesIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/times-icon"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var WizardHeader = function WizardHeader(_ref) {
  var _ref$onClose = _ref.onClose,
      onClose = _ref$onClose === void 0 ? function () {
    return undefined;
  } : _ref$onClose,
      title = _ref.title,
      description = _ref.description,
      ariaLabelCloseButton = _ref.ariaLabelCloseButton,
      titleId = _ref.titleId,
      descriptionId = _ref.descriptionId;
  return React.createElement("div", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardHeader)
  }, React.createElement(_Button.Button, {
    variant: "plain",
    className: (0, _reactStyles.css)(_wizard["default"].wizardClose),
    "aria-label": ariaLabelCloseButton,
    onClick: onClose
  }, React.createElement(_timesIcon["default"], {
    "aria-hidden": "true"
  })), React.createElement(_Title.Title, {
    size: "3xl",
    className: (0, _reactStyles.css)(_wizard["default"].wizardTitle),
    "aria-label": title,
    id: titleId
  }, title || React.createElement(React.Fragment, null, "\xA0")), description && React.createElement("p", {
    className: (0, _reactStyles.css)(_wizard["default"].wizardDescription),
    id: descriptionId
  }, description));
};

exports.WizardHeader = WizardHeader;
WizardHeader.propTypes = {
  onClose: _propTypes["default"].func,
  title: _propTypes["default"].string.isRequired,
  description: _propTypes["default"].string,
  ariaLabelCloseButton: _propTypes["default"].string,
  titleId: _propTypes["default"].string,
  descriptionId: _propTypes["default"].string
};
//# sourceMappingURL=WizardHeader.js.map