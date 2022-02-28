(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "./ChipButton", "../Tooltip", "@patternfly/react-icons/dist/js/icons/times-circle-icon", "@patternfly/react-styles/css/components/Chip/chip", "../../helpers/GenerateId/GenerateId", "../withOuia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("./ChipButton"), require("../Tooltip"), require("@patternfly/react-icons/dist/js/icons/times-circle-icon"), require("@patternfly/react-styles/css/components/Chip/chip"), require("../../helpers/GenerateId/GenerateId"), require("../withOuia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.ChipButton, global.Tooltip, global.timesCircleIcon, global.chip, global.GenerateId, global.withOuia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _ChipButton, _Tooltip, _timesCircleIcon, _chip, _GenerateId, _withOuia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Chip = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _timesCircleIcon2 = _interopRequireDefault(_timesCircleIcon);

  var _chip2 = _interopRequireDefault(_chip);

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

  class Chip extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "span", React.createRef());

      _defineProperty(this, "renderOverflowChip", () => {
        const {
          children,
          className,
          onClick,
          ouiaContext,
          ouiaId
        } = this.props;
        const Component = this.props.component;
        return React.createElement(Component, _extends({
          className: (0, _reactStyles.css)(_chip2.default.chip, _chip2.default.modifiers.overflow, className)
        }, ouiaContext.isOuia && {
          'data-ouia-component-type': 'OverflowChip',
          'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
        }), React.createElement(_ChipButton.ChipButton, {
          onClick: onClick
        }, React.createElement("span", {
          className: (0, _reactStyles.css)(_chip2.default.chipText)
        }, children)));
      });

      _defineProperty(this, "renderChip", randomId => {
        const {
          children,
          closeBtnAriaLabel,
          tooltipPosition,
          className,
          onClick,
          isReadOnly,
          ouiaContext,
          ouiaId
        } = this.props;
        const Component = this.props.component;

        if (this.state.isTooltipVisible) {
          return React.createElement(_Tooltip.Tooltip, {
            position: tooltipPosition,
            content: children
          }, React.createElement(Component, _extends({
            className: (0, _reactStyles.css)(_chip2.default.chip, isReadOnly && _chip2.default.modifiers.readOnly, className),
            tabIndex: "0"
          }, ouiaContext.isOuia && {
            'data-ouia-component-type': 'Chip',
            'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
          }), React.createElement("span", {
            ref: this.span,
            className: (0, _reactStyles.css)(_chip2.default.chipText),
            id: randomId
          }, children), !isReadOnly && React.createElement(_ChipButton.ChipButton, {
            onClick: onClick,
            ariaLabel: closeBtnAriaLabel,
            id: `remove_${randomId}`,
            "aria-labelledby": `remove_${randomId} ${randomId}`
          }, React.createElement(_timesCircleIcon2.default, {
            "aria-hidden": "true"
          }))));
        }

        return React.createElement(Component, _extends({
          className: (0, _reactStyles.css)(_chip2.default.chip, isReadOnly && _chip2.default.modifiers.readOnly, className)
        }, ouiaContext.isOuia && {
          'data-ouia-component-type': 'Chip',
          'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
        }), React.createElement("span", {
          ref: this.span,
          className: (0, _reactStyles.css)(_chip2.default.chipText),
          id: randomId
        }, children), !isReadOnly && React.createElement(_ChipButton.ChipButton, {
          onClick: onClick,
          ariaLabel: closeBtnAriaLabel,
          id: `remove_${randomId}`,
          "aria-labelledby": `remove_${randomId} ${randomId}`
        }, React.createElement(_timesCircleIcon2.default, {
          "aria-hidden": "true"
        })));
      });

      this.state = {
        isTooltipVisible: false
      };
    }

    componentDidMount() {
      this.setState({
        isTooltipVisible: Boolean(this.span.current && this.span.current.offsetWidth < this.span.current.scrollWidth)
      });
    }

    render() {
      const {
        isOverflowChip
      } = this.props;
      return React.createElement(_GenerateId2.default, null, randomId => isOverflowChip ? this.renderOverflowChip() : this.renderChip(randomId));
    }

  }

  _defineProperty(Chip, "propTypes", {
    children: _propTypes2.default.node,
    closeBtnAriaLabel: _propTypes2.default.string,
    className: _propTypes2.default.string,
    isOverflowChip: _propTypes2.default.bool,
    isReadOnly: _propTypes2.default.bool,
    onClick: _propTypes2.default.func,
    component: _propTypes2.default.node,
    tooltipPosition: _propTypes2.default.oneOf(['auto', 'top', 'bottom', 'left', 'right'])
  });

  _defineProperty(Chip, "defaultProps", {
    closeBtnAriaLabel: 'close',
    className: '',
    isOverflowChip: false,
    isReadOnly: false,
    tooltipPosition: 'top',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: _e => undefined,
    component: 'div'
  });

  const ChipWithOuiaContext = (0, _withOuia.withOuiaContext)(Chip);
  exports.Chip = ChipWithOuiaContext;
});
//# sourceMappingURL=Chip.js.map