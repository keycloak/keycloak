(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy", "@patternfly/react-styles", "../TextInput", "../Tooltip", "../../helpers/GenerateId/GenerateId", "./ClipboardCopyButton", "./ClipboardCopyToggle", "./ClipboardCopyExpanded"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy"), require("@patternfly/react-styles"), require("../TextInput"), require("../Tooltip"), require("../../helpers/GenerateId/GenerateId"), require("./ClipboardCopyButton"), require("./ClipboardCopyToggle"), require("./ClipboardCopyExpanded"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.clipboardCopy, global.reactStyles, global.TextInput, global.Tooltip, global.GenerateId, global.ClipboardCopyButton, global.ClipboardCopyToggle, global.ClipboardCopyExpanded);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _clipboardCopy, _reactStyles, _TextInput, _Tooltip, _GenerateId, _ClipboardCopyButton, _ClipboardCopyToggle, _ClipboardCopyExpanded) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ClipboardCopy = exports.ClipboardCopyVariant = exports.clipboardCopyFunc = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _clipboardCopy2 = _interopRequireDefault(_clipboardCopy);

  var _GenerateId2 = _interopRequireDefault(_GenerateId);

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

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
  }

  const clipboardCopyFunc = exports.clipboardCopyFunc = (event, text) => {
    const clipboard = event.currentTarget.parentElement;
    const el = document.createElement('input');
    el.value = text.toString();
    clipboard.appendChild(el);
    el.select();
    document.execCommand('copy');
    clipboard.removeChild(el);
  };

  let ClipboardCopyVariant = exports.ClipboardCopyVariant = undefined;

  (function (ClipboardCopyVariant) {
    ClipboardCopyVariant["inline"] = "inline";
    ClipboardCopyVariant["expansion"] = "expansion";
  })(ClipboardCopyVariant || (exports.ClipboardCopyVariant = ClipboardCopyVariant = {}));

  class ClipboardCopy extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "timer", null);

      _defineProperty(this, "componentDidUpdate", (prevProps, prevState) => {
        if (prevProps.children !== this.props.children) {
          this.updateText(this.props.children);
        }
      });

      _defineProperty(this, "expandContent", _event => {
        this.setState(prevState => ({
          expanded: !prevState.expanded
        }));
      });

      _defineProperty(this, "updateText", text => {
        this.setState({
          text
        });
        this.props.onChange(text);
      });

      _defineProperty(this, "render", () => {
        const _this$props = this.props,
              {
          /* eslint-disable @typescript-eslint/no-unused-vars */
          isExpanded,
          onChange,
          // Don't pass to <div>

          /* eslint-enable @typescript-eslint/no-unused-vars */
          isReadOnly,
          isCode,
          exitDelay,
          maxWidth,
          entryDelay,
          switchDelay,
          onCopy,
          hoverTip,
          clickTip,
          textAriaLabel,
          toggleAriaLabel,
          variant,
          position,
          className
        } = _this$props,
              divProps = _objectWithoutProperties(_this$props, ["isExpanded", "onChange", "isReadOnly", "isCode", "exitDelay", "maxWidth", "entryDelay", "switchDelay", "onCopy", "hoverTip", "clickTip", "textAriaLabel", "toggleAriaLabel", "variant", "position", "className"]);

        const textIdPrefix = 'text-input-';
        const toggleIdPrefix = 'toggle-';
        const contentIdPrefix = 'content-';
        return React.createElement("div", _extends({
          className: (0, _reactStyles.css)(_clipboardCopy2.default.clipboardCopy, this.state.expanded && _clipboardCopy2.default.modifiers.expanded, className)
        }, divProps), React.createElement(_GenerateId2.default, {
          prefix: ""
        }, id => React.createElement(React.Fragment, null, React.createElement("div", {
          className: (0, _reactStyles.css)(_clipboardCopy2.default.clipboardCopyGroup)
        }, variant === 'expansion' && React.createElement(_ClipboardCopyToggle.ClipboardCopyToggle, {
          isExpanded: this.state.expanded,
          onClick: this.expandContent,
          id: `${toggleIdPrefix}-${id}`,
          textId: `${textIdPrefix}-${id}`,
          contentId: `${contentIdPrefix}-${id}`,
          "aria-label": toggleAriaLabel
        }), React.createElement(_TextInput.TextInput, {
          isReadOnly: isReadOnly || this.state.expanded,
          onChange: this.updateText,
          value: this.state.text,
          id: `text-input-${id}`,
          "aria-label": textAriaLabel
        }), React.createElement(_ClipboardCopyButton.ClipboardCopyButton, {
          exitDelay: exitDelay,
          entryDelay: entryDelay,
          maxWidth: maxWidth,
          position: position,
          id: `copy-button-${id}`,
          textId: `text-input-${id}`,
          "aria-label": hoverTip,
          onClick: event => {
            if (this.timer) {
              window.clearTimeout(this.timer);
              this.setState({
                copied: false
              });
            }

            onCopy(event, this.state.text);
            this.setState({
              copied: true
            }, () => {
              this.timer = window.setTimeout(() => {
                this.setState({
                  copied: false
                });
                this.timer = null;
              }, switchDelay);
            });
          }
        }, this.state.copied ? clickTip : hoverTip)), this.state.expanded && React.createElement(_ClipboardCopyExpanded.ClipboardCopyExpanded, {
          isReadOnly: isReadOnly,
          isCode: isCode,
          id: `content-${id}`,
          onChange: this.updateText
        }, this.state.text))));
      });

      this.state = {
        text: this.props.children,
        expanded: this.props.isExpanded,
        copied: false
      };
    }

  }

  exports.ClipboardCopy = ClipboardCopy;

  _defineProperty(ClipboardCopy, "propTypes", {
    className: _propTypes2.default.string,
    hoverTip: _propTypes2.default.string,
    clickTip: _propTypes2.default.string,
    textAriaLabel: _propTypes2.default.string,
    toggleAriaLabel: _propTypes2.default.string,
    isReadOnly: _propTypes2.default.bool,
    isExpanded: _propTypes2.default.bool,
    isCode: _propTypes2.default.bool,
    variant: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf(['inline']), _propTypes2.default.oneOf(['expansion'])]),
    position: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf(['auto']), _propTypes2.default.oneOf(['top']), _propTypes2.default.oneOf(['bottom']), _propTypes2.default.oneOf(['left']), _propTypes2.default.oneOf(['right'])]),
    maxWidth: _propTypes2.default.string,
    exitDelay: _propTypes2.default.number,
    entryDelay: _propTypes2.default.number,
    switchDelay: _propTypes2.default.number,
    onCopy: _propTypes2.default.func,
    onChange: _propTypes2.default.func,
    children: _propTypes2.default.node.isRequired
  });

  _defineProperty(ClipboardCopy, "defaultProps", {
    hoverTip: 'Copy to clipboard',
    clickTip: 'Successfully copied to clipboard!',
    isReadOnly: false,
    isExpanded: false,
    isCode: false,
    variant: 'inline',
    position: _Tooltip.TooltipPosition.top,
    maxWidth: '150px',
    exitDelay: 1600,
    entryDelay: 100,
    switchDelay: 2000,
    onCopy: clipboardCopyFunc,
    onChange: () => undefined,
    textAriaLabel: 'Copyable input',
    toggleAriaLabel: 'Show content'
  });
});
//# sourceMappingURL=ClipboardCopy.js.map