(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Wizard/wizard", "../Button"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Wizard/wizard"), require("../Button"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.wizard, global.Button);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _wizard, _Button) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.WizardFooterInternal = undefined;

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

  const WizardFooterInternal = exports.WizardFooterInternal = ({
    onNext,
    onBack,
    onClose,
    isValid,
    firstStep,
    activeStep,
    nextButtonText,
    backButtonText,
    cancelButtonText
  }) => React.createElement("footer", {
    className: (0, _reactStyles.css)(_wizard2.default.wizardFooter)
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

  WizardFooterInternal.propTypes = {
    onNext: _propTypes2.default.any.isRequired,
    onBack: _propTypes2.default.any.isRequired,
    onClose: _propTypes2.default.any.isRequired,
    isValid: _propTypes2.default.bool.isRequired,
    firstStep: _propTypes2.default.bool.isRequired,
    activeStep: _propTypes2.default.any.isRequired,
    nextButtonText: _propTypes2.default.string.isRequired,
    backButtonText: _propTypes2.default.string.isRequired,
    cancelButtonText: _propTypes2.default.string.isRequired
  };
});
//# sourceMappingURL=WizardFooterInternal.js.map