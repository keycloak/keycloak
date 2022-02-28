(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Wizard/wizard", "@patternfly/react-styles", "../Button", "../Title", "@patternfly/react-icons/dist/js/icons/times-icon"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Wizard/wizard"), require("@patternfly/react-styles"), require("../Button"), require("../Title"), require("@patternfly/react-icons/dist/js/icons/times-icon"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.wizard, global.reactStyles, global.Button, global.Title, global.timesIcon);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _wizard, _reactStyles, _Button, _Title, _timesIcon) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.WizardHeader = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _wizard2 = _interopRequireDefault(_wizard);

  var _timesIcon2 = _interopRequireDefault(_timesIcon);

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

  const WizardHeader = exports.WizardHeader = ({
    onClose = () => undefined,
    title,
    description,
    ariaLabelCloseButton,
    titleId,
    descriptionId
  }) => React.createElement("div", {
    className: (0, _reactStyles.css)(_wizard2.default.wizardHeader)
  }, React.createElement(_Button.Button, {
    variant: "plain",
    className: (0, _reactStyles.css)(_wizard2.default.wizardClose),
    "aria-label": ariaLabelCloseButton,
    onClick: onClose
  }, React.createElement(_timesIcon2.default, {
    "aria-hidden": "true"
  })), React.createElement(_Title.Title, {
    size: "3xl",
    className: (0, _reactStyles.css)(_wizard2.default.wizardTitle),
    "aria-label": title,
    id: titleId
  }, title || React.createElement(React.Fragment, null, "\xA0")), description && React.createElement("p", {
    className: (0, _reactStyles.css)(_wizard2.default.wizardDescription),
    id: descriptionId
  }, description));

  WizardHeader.propTypes = {
    onClose: _propTypes2.default.func,
    title: _propTypes2.default.string.isRequired,
    description: _propTypes2.default.string,
    ariaLabelCloseButton: _propTypes2.default.string,
    titleId: _propTypes2.default.string,
    descriptionId: _propTypes2.default.string
  };
});
//# sourceMappingURL=WizardHeader.js.map