(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/ChipGroup/chip-group", "@patternfly/react-styles", "./Chip", "../../helpers"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/ChipGroup/chip-group"), require("@patternfly/react-styles"), require("./Chip"), require("../../helpers"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.chipGroup, global.reactStyles, global.Chip, global.helpers);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _chipGroup, _reactStyles, _Chip, _helpers) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ChipGroup = exports.ChipGroupContext = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _chipGroup2 = _interopRequireDefault(_chipGroup);

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

  const ChipGroupContext = exports.ChipGroupContext = React.createContext('');

  class ChipGroup extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "toggleCollapse", () => {
        this.setState(prevState => ({
          isOpen: !prevState.isOpen
        }));
      });

      this.state = {
        isOpen: this.props.defaultIsOpen
      };
    }

    renderToolbarGroup() {
      const {
        isOpen
      } = this.state;
      const {
        headingLevel = 'h4'
      } = this.props;
      return React.createElement(ChipGroupContext.Provider, {
        value: headingLevel
      }, React.createElement(InnerChipGroup, _extends({}, this.props, {
        isOpen: isOpen,
        onToggleCollapse: this.toggleCollapse
      })));
    }

    renderChipGroup() {
      const {
        className
      } = this.props;
      const {
        isOpen
      } = this.state;
      return React.createElement("ul", {
        className: (0, _reactStyles.css)(_chipGroup2.default.chipGroup, className)
      }, React.createElement(InnerChipGroup, _extends({}, this.props, {
        isOpen: isOpen,
        onToggleCollapse: this.toggleCollapse
      })));
    }

    render() {
      const {
        withToolbar,
        children
      } = this.props;

      if (React.Children.count(children)) {
        return withToolbar ? this.renderToolbarGroup() : this.renderChipGroup();
      }

      return null;
    }

  }

  exports.ChipGroup = ChipGroup;

  _defineProperty(ChipGroup, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    defaultIsOpen: _propTypes2.default.bool,
    expandedText: _propTypes2.default.string,
    collapsedText: _propTypes2.default.string,
    withToolbar: _propTypes2.default.bool,
    headingLevel: _propTypes2.default.oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6']),
    numChips: _propTypes2.default.number
  });

  _defineProperty(ChipGroup, "defaultProps", {
    className: '',
    expandedText: 'Show Less',
    collapsedText: '${remaining} more',
    withToolbar: false,
    defaultIsOpen: false,
    numChips: 3
  });

  const InnerChipGroup = props => {
    const {
      children,
      expandedText,
      isOpen,
      onToggleCollapse,
      collapsedText,
      withToolbar,
      numChips
    } = props;
    const collapsedTextResult = (0, _helpers.fillTemplate)(collapsedText, {
      remaining: React.Children.count(children) - numChips
    });
    const mappedChildren = React.Children.map(children, c => {
      const child = c;

      if (withToolbar) {
        return React.cloneElement(child, {
          children: React.Children.toArray(child.props.children).map(chip => React.cloneElement(chip, {
            component: 'li'
          }))
        });
      }

      return React.cloneElement(child, {
        component: 'li'
      });
    });
    return React.createElement(React.Fragment, null, isOpen ? React.createElement(React.Fragment, null, mappedChildren) : React.createElement(React.Fragment, null, mappedChildren.map((child, i) => {
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
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    defaultIsOpen: _propTypes2.default.bool,
    expandedText: _propTypes2.default.string,
    collapsedText: _propTypes2.default.string,
    withToolbar: _propTypes2.default.bool,
    headingLevel: _propTypes2.default.oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6']),
    numChips: _propTypes2.default.number,
    isOpen: _propTypes2.default.bool.isRequired,
    onToggleCollapse: _propTypes2.default.func.isRequired
  };
});
//# sourceMappingURL=ChipGroup.js.map