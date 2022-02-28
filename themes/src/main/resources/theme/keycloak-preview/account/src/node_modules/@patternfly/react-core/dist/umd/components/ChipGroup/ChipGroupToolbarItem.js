(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/ChipGroup/chip-group", "./ChipGroup", "./ChipButton", "../Tooltip", "@patternfly/react-icons/dist/js/icons/times-icon", "../../helpers/GenerateId/GenerateId"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/ChipGroup/chip-group"), require("./ChipGroup"), require("./ChipButton"), require("../Tooltip"), require("@patternfly/react-icons/dist/js/icons/times-icon"), require("../../helpers/GenerateId/GenerateId"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.chipGroup, global.ChipGroup, global.ChipButton, global.Tooltip, global.timesIcon, global.GenerateId);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _chipGroup, _ChipGroup, _ChipButton, _Tooltip, _timesIcon, _GenerateId) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ChipGroupToolbarItem = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _chipGroup2 = _interopRequireDefault(_chipGroup);

  var _timesIcon2 = _interopRequireDefault(_timesIcon);

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

  class ChipGroupToolbarItem extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "heading", React.createRef());

      this.state = {
        isTooltipVisible: false
      };
    }

    componentDidMount() {
      this.setState({
        isTooltipVisible: Boolean(this.heading.current && this.heading.current.offsetWidth < this.heading.current.scrollWidth)
      });
    }

    render() {
      const _this$props = this.props,
            {
        categoryName,
        children,
        className,
        isClosable,
        closeBtnAriaLabel,
        onClick,
        tooltipPosition
      } = _this$props,
            rest = _objectWithoutProperties(_this$props, ["categoryName", "children", "className", "isClosable", "closeBtnAriaLabel", "onClick", "tooltipPosition"]);

      if (React.Children.count(children)) {
        const renderChipGroup = (id, HeadingLevel) => React.createElement("ul", _extends({
          className: (0, _reactStyles.css)(_chipGroup2.default.chipGroup, _chipGroup2.default.modifiers.toolbar, className)
        }, rest), React.createElement("li", null, this.state.isTooltipVisible ? React.createElement(_Tooltip.Tooltip, {
          position: tooltipPosition,
          content: categoryName
        }, React.createElement(HeadingLevel, {
          tabIndex: "0",
          ref: this.heading,
          className: (0, _reactStyles.css)(_chipGroup2.default.chipGroupLabel),
          id: id
        }, categoryName)) : React.createElement(HeadingLevel, {
          ref: this.heading,
          className: (0, _reactStyles.css)(_chipGroup2.default.chipGroupLabel),
          id: id
        }, categoryName), React.createElement("ul", {
          className: (0, _reactStyles.css)(_chipGroup2.default.chipGroup)
        }, children), isClosable && React.createElement("div", {
          className: "pf-c-chip-group__close"
        }, React.createElement(_ChipButton.ChipButton, {
          "aria-label": closeBtnAriaLabel,
          onClick: onClick,
          id: `remove_group_${id}`,
          "aria-labelledby": `remove_group_${id} ${id}`
        }, React.createElement(_timesIcon2.default, {
          "aria-hidden": "true"
        })))));

        return React.createElement(_ChipGroup.ChipGroupContext.Consumer, null, HeadingLevel => React.createElement(_GenerateId2.default, null, randomId => renderChipGroup(randomId, HeadingLevel)));
      }

      return null;
    }

  }

  exports.ChipGroupToolbarItem = ChipGroupToolbarItem;

  _defineProperty(ChipGroupToolbarItem, "propTypes", {
    categoryName: _propTypes2.default.string,
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    isClosable: _propTypes2.default.bool,
    onClick: _propTypes2.default.func,
    closeBtnAriaLabel: _propTypes2.default.string,
    tooltipPosition: _propTypes2.default.oneOf(['auto', 'top', 'bottom', 'left', 'right'])
  });

  _defineProperty(ChipGroupToolbarItem, "defaultProps", {
    categoryName: '',
    children: null,
    className: '',
    isClosable: false,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: _e => undefined,
    closeBtnAriaLabel: 'Close chip group',
    tooltipPosition: 'top'
  });
});
//# sourceMappingURL=ChipGroupToolbarItem.js.map