(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/OptionsMenu/options-menu", "@patternfly/react-styles/css/components/Pagination/pagination", "@patternfly/react-styles", "../Dropdown", "@patternfly/react-icons/dist/js/icons/check-icon", "./OptionsToggle"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"), require("@patternfly/react-styles/css/components/Pagination/pagination"), require("@patternfly/react-styles"), require("../Dropdown"), require("@patternfly/react-icons/dist/js/icons/check-icon"), require("./OptionsToggle"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.optionsMenu, global.pagination, global.reactStyles, global.Dropdown, global.checkIcon, global.OptionsToggle);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _optionsMenu, _pagination, _reactStyles, _Dropdown, _checkIcon, _OptionsToggle) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.PaginationOptionsMenu = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _optionsMenu2 = _interopRequireDefault(_optionsMenu);

  var _pagination2 = _interopRequireDefault(_pagination);

  var _checkIcon2 = _interopRequireDefault(_checkIcon);

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

  class PaginationOptionsMenu extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "parentRef", React.createRef());

      _defineProperty(this, "onToggle", isOpen => {
        this.setState({
          isOpen
        });
      });

      _defineProperty(this, "onSelect", () => {
        this.setState(prevState => ({
          isOpen: !prevState.isOpen
        }));
      });

      _defineProperty(this, "handleNewPerPage", (_evt, newPerPage) => {
        const {
          page,
          onPerPageSelect,
          itemCount,
          defaultToFullPage
        } = this.props;
        let newPage = page;

        while (Math.ceil(itemCount / newPerPage) < newPage) {
          newPage--;
        }

        if (defaultToFullPage) {
          if (itemCount / newPerPage !== newPage) {
            while (newPage > 1 && itemCount - newPerPage * newPage < 0) {
              newPage--;
            }
          }
        }

        const startIdx = (newPage - 1) * newPerPage;
        const endIdx = newPage * newPerPage;
        return onPerPageSelect(_evt, newPerPage, newPage, startIdx, endIdx);
      });

      _defineProperty(this, "renderItems", () => {
        const {
          perPageOptions,
          perPage,
          perPageSuffix
        } = this.props;
        return perPageOptions.map(({
          value,
          title
        }) => React.createElement(_Dropdown.DropdownItem, {
          key: value,
          component: "button",
          "data-action": `per-page-${value}`,
          className: (0, _reactStyles.css)(perPage === value && 'pf-m-selected'),
          onClick: event => this.handleNewPerPage(event, value)
        }, title, React.createElement("span", {
          className: (0, _reactStyles.css)(_pagination2.default.paginationMenuText)
        }, ` ${perPageSuffix}`), perPage === value && React.createElement("i", {
          className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuMenuItemIcon)
        }, React.createElement(_checkIcon2.default, null))));
      });

      this.state = {
        isOpen: false
      };
    }

    render() {
      const {
        widgetId,
        isDisabled,
        itemsPerPageTitle,
        dropDirection,
        optionsToggle,
        perPageOptions,
        toggleTemplate,
        firstIndex,
        lastIndex,
        itemCount,
        itemsTitle
      } = this.props;
      const {
        isOpen
      } = this.state;
      return React.createElement(_Dropdown.DropdownContext.Provider, {
        value: {
          id: widgetId,
          onSelect: this.onSelect,
          toggleIconClass: _optionsMenu2.default.optionsMenuToggleIcon,
          toggleTextClass: _optionsMenu2.default.optionsMenuToggleText,
          menuClass: _optionsMenu2.default.optionsMenuMenu,
          itemClass: _optionsMenu2.default.optionsMenuMenuItem,
          toggleClass: ' ',
          baseClass: _optionsMenu2.default.optionsMenu,
          disabledClass: _optionsMenu2.default.modifiers.disabled,
          menuComponent: 'ul',
          baseComponent: 'div'
        }
      }, React.createElement(_Dropdown.DropdownWithContext, {
        direction: dropDirection,
        isOpen: isOpen,
        toggle: React.createElement(_OptionsToggle.OptionsToggle, {
          optionsToggle: optionsToggle,
          itemsPerPageTitle: itemsPerPageTitle,
          showToggle: perPageOptions && perPageOptions.length > 0,
          onToggle: this.onToggle,
          isOpen: isOpen,
          widgetId: widgetId,
          firstIndex: firstIndex,
          lastIndex: lastIndex,
          itemCount: itemCount,
          itemsTitle: itemsTitle,
          toggleTemplate: toggleTemplate,
          parentRef: this.parentRef.current,
          isDisabled: isDisabled
        }),
        dropdownItems: this.renderItems(),
        isPlain: true
      }));
    }

  }

  exports.PaginationOptionsMenu = PaginationOptionsMenu;

  _defineProperty(PaginationOptionsMenu, "propTypes", {
    className: _propTypes2.default.string,
    widgetId: _propTypes2.default.string,
    isDisabled: _propTypes2.default.bool,
    dropDirection: _propTypes2.default.oneOf(['up', 'down']),
    perPageOptions: _propTypes2.default.arrayOf(_propTypes2.default.any),
    itemsPerPageTitle: _propTypes2.default.string,
    page: _propTypes2.default.number,
    perPageSuffix: _propTypes2.default.string,
    itemsTitle: _propTypes2.default.string,
    optionsToggle: _propTypes2.default.string,
    itemCount: _propTypes2.default.number,
    firstIndex: _propTypes2.default.number,
    lastIndex: _propTypes2.default.number,
    defaultToFullPage: _propTypes2.default.bool,
    perPage: _propTypes2.default.number,
    lastPage: _propTypes2.default.number,
    toggleTemplate: _propTypes2.default.oneOfType([_propTypes2.default.func, _propTypes2.default.string]),
    onPerPageSelect: _propTypes2.default.any
  });

  _defineProperty(PaginationOptionsMenu, "defaultProps", {
    className: '',
    widgetId: '',
    isDisabled: false,
    dropDirection: _Dropdown.DropdownDirection.down,
    perPageOptions: [],
    itemsPerPageTitle: 'Items per page',
    perPageSuffix: 'per page',
    optionsToggle: 'Select',
    perPage: 0,
    firstIndex: 0,
    lastIndex: 0,
    defaultToFullPage: false,
    itemCount: 0,
    itemsTitle: 'items',
    toggleTemplate: ({
      firstIndex,
      lastIndex,
      itemCount,
      itemsTitle
    }) => React.createElement(React.Fragment, null, React.createElement("b", null, firstIndex, " - ", lastIndex), ' ', "of", React.createElement("b", null, itemCount), " ", itemsTitle),
    onPerPageSelect: () => null
  });
});
//# sourceMappingURL=PaginationOptionsMenu.js.map