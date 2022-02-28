"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DataListItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _dataList = _interopRequireDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));

var _DataList = require("./DataList");

var _Select = require("../Select");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var DataListItem = function DataListItem(_ref) {
  var _ref$isExpanded = _ref.isExpanded,
      isExpanded = _ref$isExpanded === void 0 ? false : _ref$isExpanded,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$id = _ref.id,
      id = _ref$id === void 0 ? '' : _ref$id,
      ariaLabelledBy = _ref['aria-labelledby'],
      children = _ref.children,
      props = _objectWithoutProperties(_ref, ["isExpanded", "className", "id", "aria-labelledby", "children"]);

  return React.createElement(_DataList.DataListContext.Consumer, null, function (_ref2) {
    var isSelectable = _ref2.isSelectable,
        selectedDataListItemId = _ref2.selectedDataListItemId,
        updateSelectedDataListItem = _ref2.updateSelectedDataListItem;

    var selectDataListItem = function selectDataListItem(event) {
      var target = event.target;

      while (event.currentTarget !== target) {
        if ('onclick' in target && target.onclick || target.parentNode.classList.contains(_dataList["default"].dataListItemAction) || target.parentNode.classList.contains(_dataList["default"].dataListItemControl)) {
          // check other event handlers are not present.
          return;
        } else {
          target = target.parentNode;
        }
      }

      updateSelectedDataListItem(id);
    };

    var onKeyDown = function onKeyDown(event) {
      if (event.key === _Select.KeyTypes.Enter) {
        updateSelectedDataListItem(id);
      }
    };

    return React.createElement("li", _extends({
      id: id,
      className: (0, _reactStyles.css)(_dataList["default"].dataListItem, isExpanded && _dataList["default"].modifiers.expanded, isSelectable && _dataList["default"].modifiers.selectable, selectedDataListItemId && selectedDataListItemId === id && _dataList["default"].modifiers.selected, className),
      "aria-labelledby": ariaLabelledBy
    }, isSelectable && {
      tabIndex: 0,
      onClick: selectDataListItem,
      onKeyDown: onKeyDown
    }, isSelectable && selectedDataListItemId === id && {
      'aria-selected': true
    }, props), React.Children.map(children, function (child) {
      return React.isValidElement(child) && React.cloneElement(child, {
        rowid: ariaLabelledBy
      });
    }));
  });
};

exports.DataListItem = DataListItem;
DataListItem.propTypes = {
  isExpanded: _propTypes["default"].bool,
  children: _propTypes["default"].node.isRequired,
  className: _propTypes["default"].string,
  'aria-labelledby': _propTypes["default"].string.isRequired,
  id: _propTypes["default"].string
};
//# sourceMappingURL=DataListItem.js.map