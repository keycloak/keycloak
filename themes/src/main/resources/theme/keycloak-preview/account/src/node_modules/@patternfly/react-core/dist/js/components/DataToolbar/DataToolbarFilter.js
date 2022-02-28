"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DataToolbarFilter = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var ReactDOM = _interopRequireWildcard(require("react-dom"));

var _DataToolbarItem = require("./DataToolbarItem");

var _ChipGroup = require("../../components/ChipGroup");

var _DataToolbarUtils = require("./DataToolbarUtils");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var DataToolbarFilter =
/*#__PURE__*/
function (_React$Component) {
  _inherits(DataToolbarFilter, _React$Component);

  function DataToolbarFilter(props) {
    var _this;

    _classCallCheck(this, DataToolbarFilter);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(DataToolbarFilter).call(this, props));
    _this.state = {
      isMounted: false
    };
    return _this;
  }

  _createClass(DataToolbarFilter, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      var _this$props = this.props,
          categoryName = _this$props.categoryName,
          chips = _this$props.chips;
      this.context.updateNumberFilters(typeof categoryName === 'string' ? categoryName : categoryName.name, chips.length);
      this.setState({
        isMounted: true
      });
    }
  }, {
    key: "componentDidUpdate",
    value: function componentDidUpdate() {
      var _this$props2 = this.props,
          categoryName = _this$props2.categoryName,
          chips = _this$props2.chips;
      this.context.updateNumberFilters(typeof categoryName === 'string' ? categoryName : categoryName.name, chips.length);
    }
  }, {
    key: "render",
    value: function render() {
      var _this$props3 = this.props,
          children = _this$props3.children,
          chips = _this$props3.chips,
          deleteChip = _this$props3.deleteChip,
          categoryName = _this$props3.categoryName,
          showToolbarItem = _this$props3.showToolbarItem,
          props = _objectWithoutProperties(_this$props3, ["children", "chips", "deleteChip", "categoryName", "showToolbarItem"]);

      var _this$context = this.context,
          isExpanded = _this$context.isExpanded,
          chipGroupContentRef = _this$context.chipGroupContentRef;
      var chipGroup = chips.length ? React.createElement(_DataToolbarItem.DataToolbarItem, {
        variant: "chip-group"
      }, React.createElement(_ChipGroup.ChipGroup, {
        withToolbar: true
      }, React.createElement(_ChipGroup.ChipGroupToolbarItem, {
        key: typeof categoryName === 'string' ? categoryName : categoryName.key,
        categoryName: typeof categoryName === 'string' ? categoryName : categoryName.name
      }, chips.map(function (chip) {
        return typeof chip === 'string' ? React.createElement(_ChipGroup.Chip, {
          key: chip,
          onClick: function onClick() {
            return deleteChip(categoryName, chip);
          }
        }, chip) : React.createElement(_ChipGroup.Chip, {
          key: chip.key,
          onClick: function onClick() {
            return deleteChip(categoryName, chip);
          }
        }, chip.node);
      })))) : null;

      if (!isExpanded && this.state.isMounted) {
        return React.createElement(React.Fragment, null, showToolbarItem && React.createElement(_DataToolbarItem.DataToolbarItem, props, children), ReactDOM.createPortal(chipGroup, chipGroupContentRef.current.firstElementChild));
      }

      return React.createElement(_DataToolbarUtils.DataToolbarContentContext.Consumer, null, function (_ref) {
        var chipContainerRef = _ref.chipContainerRef;
        return React.createElement(React.Fragment, null, showToolbarItem && React.createElement(_DataToolbarItem.DataToolbarItem, props, children), chipContainerRef.current && ReactDOM.createPortal(chipGroup, chipContainerRef.current));
      });
    }
  }]);

  return DataToolbarFilter;
}(React.Component);

exports.DataToolbarFilter = DataToolbarFilter;

_defineProperty(DataToolbarFilter, "propTypes", {
  chips: _propTypes["default"].arrayOf(_propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].shape({
    key: _propTypes["default"].string.isRequired,
    node: _propTypes["default"].node.isRequired
  })])),
  deleteChip: _propTypes["default"].func,
  children: _propTypes["default"].node.isRequired,
  categoryName: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].shape({
    key: _propTypes["default"].string.isRequired,
    name: _propTypes["default"].string.isRequired
  })]).isRequired,
  showToolbarItem: _propTypes["default"].bool
});

_defineProperty(DataToolbarFilter, "contextType", _DataToolbarUtils.DataToolbarContext);

_defineProperty(DataToolbarFilter, "defaultProps", {
  chips: [],
  showToolbarItem: true
});
//# sourceMappingURL=DataToolbarFilter.js.map