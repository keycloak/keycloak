"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.WizardContextConsumer = exports.WizardContextProvider = void 0;

var React = _interopRequireWildcard(require("react"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

var WizardContext = React.createContext({
  goToStepById: function goToStepById() {
    return null;
  },
  goToStepByName: function goToStepByName() {
    return null;
  },
  onNext: function onNext() {
    return null;
  },
  onBack: function onBack() {
    return null;
  },
  onClose: function onClose() {
    return null;
  },
  activeStep: {
    name: null
  }
});
var WizardContextProvider = WizardContext.Provider;
exports.WizardContextProvider = WizardContextProvider;
var WizardContextConsumer = WizardContext.Consumer;
exports.WizardContextConsumer = WizardContextConsumer;
//# sourceMappingURL=WizardContext.js.map