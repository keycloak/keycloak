(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "react-dom", "./DataToolbarItem", "../../components/ChipGroup", "./DataToolbarUtils"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("react-dom"), require("./DataToolbarItem"), require("../../components/ChipGroup"), require("./DataToolbarUtils"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactDom, global.DataToolbarItem, global.ChipGroup, global.DataToolbarUtils);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactDom, _DataToolbarItem, _ChipGroup, _DataToolbarUtils) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DataToolbarFilter = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var ReactDOM = _interopRequireWildcard(_reactDom);

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

  class DataToolbarFilter extends React.Component {
    constructor(props) {
      super(props);
      this.state = {
        isMounted: false
      };
    }

    componentDidMount() {
      const {
        categoryName,
        chips
      } = this.props;
      this.context.updateNumberFilters(typeof categoryName === 'string' ? categoryName : categoryName.name, chips.length);
      this.setState({
        isMounted: true
      });
    }

    componentDidUpdate() {
      const {
        categoryName,
        chips
      } = this.props;
      this.context.updateNumberFilters(typeof categoryName === 'string' ? categoryName : categoryName.name, chips.length);
    }

    render() {
      const _this$props = this.props,
            {
        children,
        chips,
        deleteChip,
        categoryName,
        showToolbarItem
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["children", "chips", "deleteChip", "categoryName", "showToolbarItem"]);

      const {
        isExpanded,
        chipGroupContentRef
      } = this.context;
      const chipGroup = chips.length ? React.createElement(_DataToolbarItem.DataToolbarItem, {
        variant: "chip-group"
      }, React.createElement(_ChipGroup.ChipGroup, {
        withToolbar: true
      }, React.createElement(_ChipGroup.ChipGroupToolbarItem, {
        key: typeof categoryName === 'string' ? categoryName : categoryName.key,
        categoryName: typeof categoryName === 'string' ? categoryName : categoryName.name
      }, chips.map(chip => typeof chip === 'string' ? React.createElement(_ChipGroup.Chip, {
        key: chip,
        onClick: () => deleteChip(categoryName, chip)
      }, chip) : React.createElement(_ChipGroup.Chip, {
        key: chip.key,
        onClick: () => deleteChip(categoryName, chip)
      }, chip.node))))) : null;

      if (!isExpanded && this.state.isMounted) {
        return React.createElement(React.Fragment, null, showToolbarItem && React.createElement(_DataToolbarItem.DataToolbarItem, props, children), ReactDOM.createPortal(chipGroup, chipGroupContentRef.current.firstElementChild));
      }

      return React.createElement(_DataToolbarUtils.DataToolbarContentContext.Consumer, null, ({
        chipContainerRef
      }) => React.createElement(React.Fragment, null, showToolbarItem && React.createElement(_DataToolbarItem.DataToolbarItem, props, children), chipContainerRef.current && ReactDOM.createPortal(chipGroup, chipContainerRef.current)));
    }

  }

  exports.DataToolbarFilter = DataToolbarFilter;

  _defineProperty(DataToolbarFilter, "propTypes", {
    chips: _propTypes2.default.arrayOf(_propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.shape({
      key: _propTypes2.default.string.isRequired,
      node: _propTypes2.default.node.isRequired
    })])),
    deleteChip: _propTypes2.default.func,
    children: _propTypes2.default.node.isRequired,
    categoryName: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.shape({
      key: _propTypes2.default.string.isRequired,
      name: _propTypes2.default.string.isRequired
    })]).isRequired,
    showToolbarItem: _propTypes2.default.bool
  });

  _defineProperty(DataToolbarFilter, "contextType", _DataToolbarUtils.DataToolbarContext);

  _defineProperty(DataToolbarFilter, "defaultProps", {
    chips: [],
    showToolbarItem: true
  });
});
//# sourceMappingURL=DataToolbarFilter.js.map