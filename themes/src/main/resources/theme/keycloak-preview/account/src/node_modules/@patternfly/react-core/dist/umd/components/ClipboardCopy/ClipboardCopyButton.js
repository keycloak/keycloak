(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/copy-icon", "../Tooltip"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/copy-icon"), require("../Tooltip"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.clipboardCopy, global.reactStyles, global.copyIcon, global.Tooltip);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _clipboardCopy, _reactStyles, _copyIcon, _Tooltip) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ClipboardCopyButton = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _clipboardCopy2 = _interopRequireDefault(_clipboardCopy);

  var _copyIcon2 = _interopRequireDefault(_copyIcon);

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

  function _extends() {
    _extends = Object.assign || function (target) {
      for (var i = 1; i < arguments.length; i++) {
        var source = arguments[i];

        for (var key in source) {
          if (Object.prototype.hasOwnProperty.call(source, key)) {
            target[key] = source[key];
          }
        }
      }

      return target;
    };

    return _extends.apply(this, arguments);
  }

  function _objectWithoutProperties(source, excluded) {
    if (source == null) return {};

    var target = _objectWithoutPropertiesLoose(source, excluded);

    var key, i;

    if (Object.getOwnPropertySymbols) {
      var sourceSymbolKeys = Object.getOwnPropertySymbols(source);

      for (i = 0; i < sourceSymbolKeys.length; i++) {
        key = sourceSymbolKeys[i];
        if (excluded.indexOf(key) >= 0) continue;
        if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue;
        target[key] = source[key];
      }
    }

    return target;
  }

  function _objectWithoutPropertiesLoose(source, excluded) {
    if (source == null) return {};
    var target = {};
    var sourceKeys = Object.keys(source);
    var key, i;

    for (i = 0; i < sourceKeys.length; i++) {
      key = sourceKeys[i];
      if (excluded.indexOf(key) >= 0) continue;
      target[key] = source[key];
    }

    return target;
  }

  const ClipboardCopyButton = exports.ClipboardCopyButton = _ref => {
    let {
      onClick,
      className = '',
      exitDelay = 100,
      entryDelay = 100,
      maxWidth = '100px',
      position = 'top',
      'aria-label': ariaLabel = 'Copyable input',
      id,
      textId,
      children
    } = _ref,
        props = _objectWithoutProperties(_ref, ["onClick", "className", "exitDelay", "entryDelay", "maxWidth", "position", "aria-label", "id", "textId", "children"]);

    return React.createElement(_Tooltip.Tooltip, {
      trigger: "mouseenter focus click",
      exitDelay: exitDelay,
      entryDelay: entryDelay,
      maxWidth: maxWidth,
      position: position,
      content: React.createElement("div", null, children)
    }, React.createElement("button", _extends({
      type: "button",
      onClick: onClick,
      className: (0, _reactStyles.css)(_clipboardCopy2.default.clipboardCopyGroupCopy, className),
      "aria-label": ariaLabel,
      id: id,
      "aria-labelledby": `${id} ${textId}`
    }, props), React.createElement(_copyIcon2.default, null)));
  };

  ClipboardCopyButton.propTypes = {
    onClick: _propTypes2.default.func.isRequired,
    children: _propTypes2.default.node.isRequired,
    id: _propTypes2.default.string.isRequired,
    textId: _propTypes2.default.string.isRequired,
    className: _propTypes2.default.string,
    exitDelay: _propTypes2.default.number,
    entryDelay: _propTypes2.default.number,
    maxWidth: _propTypes2.default.string,
    position: _propTypes2.default.oneOf(['auto', 'top', 'bottom', 'left', 'right']),
    'aria-label': _propTypes2.default.string
  };
});
//# sourceMappingURL=ClipboardCopyButton.js.map