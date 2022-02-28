"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ChipGroup = exports.ChipGroupContext = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _chipGroup = _interopRequireDefault(require("@patternfly/react-styles/css/components/ChipGroup/chip-group"));

var _reactStyles = require("@patternfly/react-styles");

var _Chip = require("./Chip");

var _helpers = require("../../helpers");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var ChipGroupContext = React.createContext('');
exports.ChipGroupContext = ChipGroupContext;

var ChipGroup =
/*#__PURE__*/
function (_React$Component) {
  _inherits(ChipGroup, _React$Component);

  function ChipGroup(props) {
    var _this;

    _classCallCheck(this, ChipGroup);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(ChipGroup).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "toggleCollapse", function () {
      _this.setState(function (prevState) {
        return {
          isOpen: !prevState.isOpen
        };
      });
    });

    _this.state = {
      isOpen: _this.props.defaultIsOpen
    };
    return _this;
  }

  _createClass(ChipGroup, [{
    key: "renderToolbarGroup",
    value: function renderToolbarGroup() {
      var isOpen = this.state.isOpen;
      var _this$props$headingLe = this.props.headingLevel,
          headingLevel = _this$props$headingLe === void 0 ? 'h4' : _this$props$headingLe;
      return React.createElement(ChipGroupContext.Provider, {
        value: headingLevel
      }, React.createElement(InnerChipGroup, _extends({}, this.props, {
        isOpen: isOpen,
        onToggleCollapse: this.toggleCollapse
      })));
    }
  }, {
    key: "renderChipGroup",
    value: function renderChipGroup() {
      var className = this.props.className;
      var isOpen = this.state.isOpen;
      return React.createElement("ul", {
        className: (0, _reactStyles.css)(_chipGroup["default"].chipGroup, className)
      }, React.createElement(InnerChipGroup, _extends({}, this.props, {
        isOpen: isOpen,
        onToggleCollapse: this.toggleCollapse
      })));
    }
  }, {
    key: "render",
    value: function render() {
      var _this$props = this.props,
          withToolbar = _this$props.withToolbar,
          children = _this$props.children;

      if (React.Children.count(children)) {
        return withToolbar ? this.renderToolbarGroup() : this.renderChipGroup();
      }

      return null;
    }
  }]);

  return ChipGroup;
}(React.Component);

exports.ChipGroup = ChipGroup;

_defineProperty(ChipGroup, "propTypes", {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  defaultIsOpen: _propTypes["default"].bool,
  expandedText: _propTypes["default"].string,
  collapsedText: _propTypes["default"].string,
  withToolbar: _propTypes["default"].bool,
  headingLevel: _propTypes["default"].oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6']),
  numChips: _propTypes["default"].number
});

_defineProperty(ChipGroup, "defaultProps", {
  className: '',
  expandedText: 'Show Less',
  collapsedText: '${remaining} more',
  withToolbar: false,
  defaultIsOpen: false,
  numChips: 3
});

var InnerChipGroup = function InnerChipGroup(props) {
  var children = props.children,
      expandedText = props.expandedText,
      isOpen = props.isOpen,
      onToggleCollapse = props.onToggleCollapse,
      collapsedText = props.collapsedText,
      withToolbar = props.withToolbar,
      numChips = props.numChips;
  var collapsedTextResult = (0, _helpers.fillTemplate)(collapsedText, {
    remaining: React.Children.count(children) - numChips
  });
  var mappedChildren = React.Children.map(children, function (c) {
    var child = c;

    if (withToolbar) {
      return React.cloneElement(child, {
        children: React.Children.toArray(child.props.children).map(function (chip) {
          return React.cloneElement(chip, {
            component: 'li'
          });
        })
      });
    }

    return React.cloneElement(child, {
      component: 'li'
    });
  });
  return React.createElement(React.Fragment, null, isOpen ? React.createElement(React.Fragment, null, mappedChildren) : React.createElement(React.Fragment, null, mappedChildren.map(function (child, i) {
    if (i < numChips) {
      return child;
    }
  })), React.Children.count(children) > numChips && React.createElement(_Chip.Chip, {
    isOverflowChip: true,
    onClick: onToggleCollapse,
    component: withToolbar ? 'div' : 'li'
  }, isOpen ? expandedText : collapsedTextResult));
};

InnerChipGroup.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  defaultIsOpen: _propTypes["default"].bool,
  expandedText: _propTypes["default"].string,
  collapsedText: _propTypes["default"].string,
  withToolbar: _propTypes["default"].bool,
  headingLevel: _propTypes["default"].oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6']),
  numChips: _propTypes["default"].number,
  isOpen: _propTypes["default"].bool.isRequired,
  onToggleCollapse: _propTypes["default"].func.isRequired
};
//# sourceMappingURL=ChipGroup.js.map