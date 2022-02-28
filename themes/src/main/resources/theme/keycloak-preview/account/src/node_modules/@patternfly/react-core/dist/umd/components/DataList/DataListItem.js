(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/DataList/data-list", "./DataList", "../Select"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/DataList/data-list"), require("./DataList"), require("../Select"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.dataList, global.DataList, global.Select);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _dataList, _DataList, _Select) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DataListItem = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _dataList2 = _interopRequireDefault(_dataList);

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

  const DataListItem = exports.DataListItem = _ref => {
    let {
      isExpanded = false,
      className = '',
      id = '',
      'aria-labelledby': ariaLabelledBy,
      children
    } = _ref,
        props = _objectWithoutProperties(_ref, ["isExpanded", "className", "id", "aria-labelledby", "children"]);

    return React.createElement(_DataList.DataListContext.Consumer, null, ({
      isSelectable,
      selectedDataListItemId,
      updateSelectedDataListItem
    }) => {
      const selectDataListItem = event => {
        let target = event.target;

        while (event.currentTarget !== target) {
          if ('onclick' in target && target.onclick || target.parentNode.classList.contains(_dataList2.default.dataListItemAction) || target.parentNode.classList.contains(_dataList2.default.dataListItemControl)) {
            // check other event handlers are not present.
            return;
          } else {
            target = target.parentNode;
          }
        }

        updateSelectedDataListItem(id);
      };

      const onKeyDown = event => {
        if (event.key === _Select.KeyTypes.Enter) {
          updateSelectedDataListItem(id);
        }
      };

      return React.createElement("li", _extends({
        id: id,
        className: (0, _reactStyles.css)(_dataList2.default.dataListItem, isExpanded && _dataList2.default.modifiers.expanded, isSelectable && _dataList2.default.modifiers.selectable, selectedDataListItemId && selectedDataListItemId === id && _dataList2.default.modifiers.selected, className),
        "aria-labelledby": ariaLabelledBy
      }, isSelectable && {
        tabIndex: 0,
        onClick: selectDataListItem,
        onKeyDown
      }, isSelectable && selectedDataListItemId === id && {
        'aria-selected': true
      }, props), React.Children.map(children, child => React.isValidElement(child) && React.cloneElement(child, {
        rowid: ariaLabelledBy
      })));
    });
  };

  DataListItem.propTypes = {
    isExpanded: _propTypes2.default.bool,
    children: _propTypes2.default.node.isRequired,
    className: _propTypes2.default.string,
    'aria-labelledby': _propTypes2.default.string.isRequired,
    id: _propTypes2.default.string
  };
});
//# sourceMappingURL=DataListItem.js.map