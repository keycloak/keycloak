(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Progress/progress", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/check-circle-icon", "@patternfly/react-icons/dist/js/icons/times-circle-icon", "./ProgressBar"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Progress/progress"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/check-circle-icon"), require("@patternfly/react-icons/dist/js/icons/times-circle-icon"), require("./ProgressBar"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.progress, global.reactStyles, global.checkCircleIcon, global.timesCircleIcon, global.ProgressBar);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _progress, _reactStyles, _checkCircleIcon, _timesCircleIcon, _ProgressBar) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ProgressContainer = exports.ProgressVariant = exports.ProgressMeasureLocation = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _progress2 = _interopRequireDefault(_progress);

  var _checkCircleIcon2 = _interopRequireDefault(_checkCircleIcon);

  var _timesCircleIcon2 = _interopRequireDefault(_timesCircleIcon);

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

  let ProgressMeasureLocation = exports.ProgressMeasureLocation = undefined;

  (function (ProgressMeasureLocation) {
    ProgressMeasureLocation["outside"] = "outside";
    ProgressMeasureLocation["inside"] = "inside";
    ProgressMeasureLocation["top"] = "top";
    ProgressMeasureLocation["none"] = "none";
  })(ProgressMeasureLocation || (exports.ProgressMeasureLocation = ProgressMeasureLocation = {}));

  let ProgressVariant = exports.ProgressVariant = undefined;

  (function (ProgressVariant) {
    ProgressVariant["danger"] = "danger";
    ProgressVariant["success"] = "success";
    ProgressVariant["info"] = "info";
  })(ProgressVariant || (exports.ProgressVariant = ProgressVariant = {}));

  const variantToIcon = {
    danger: _timesCircleIcon2.default,
    success: _checkCircleIcon2.default
  };

  const ProgressContainer = exports.ProgressContainer = ({
    ariaProps,
    value,
    title = '',
    parentId,
    label = null,
    variant = ProgressVariant.info,
    measureLocation = ProgressMeasureLocation.top
  }) => {
    const StatusIcon = variantToIcon.hasOwnProperty(variant) && variantToIcon[variant];
    return React.createElement(React.Fragment, null, React.createElement("div", {
      className: (0, _reactStyles.css)(_progress2.default.progressDescription),
      id: `${parentId}-description`
    }, title), React.createElement("div", {
      className: (0, _reactStyles.css)(_progress2.default.progressStatus)
    }, (measureLocation === ProgressMeasureLocation.top || measureLocation === ProgressMeasureLocation.outside) && React.createElement("span", {
      className: (0, _reactStyles.css)(_progress2.default.progressMeasure)
    }, label || `${value}%`), variantToIcon.hasOwnProperty(variant) && React.createElement("span", {
      className: (0, _reactStyles.css)(_progress2.default.progressStatusIcon)
    }, React.createElement(StatusIcon, null))), React.createElement(_ProgressBar.ProgressBar, {
      ariaProps: ariaProps,
      value: value
    }, measureLocation === ProgressMeasureLocation.inside && `${value}%`));
  };

  ProgressContainer.propTypes = {
    ariaProps: _propTypes2.default.any,
    parentId: _propTypes2.default.string.isRequired,
    title: _propTypes2.default.string,
    label: _propTypes2.default.node,
    variant: _propTypes2.default.oneOf(['danger', 'success', 'info']),
    measureLocation: _propTypes2.default.oneOf(['outside', 'inside', 'top', 'none']),
    value: _propTypes2.default.number.isRequired
  };
});
//# sourceMappingURL=ProgressContainer.js.map