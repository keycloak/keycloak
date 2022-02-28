(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "../Dropdown", "./ToggleTemplate", "@patternfly/react-styles/css/components/Pagination/pagination", "@patternfly/react-styles", "./Navigation", "./PaginationOptionsMenu", "../withOuia", "@patternfly/react-tokens"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("../Dropdown"), require("./ToggleTemplate"), require("@patternfly/react-styles/css/components/Pagination/pagination"), require("@patternfly/react-styles"), require("./Navigation"), require("./PaginationOptionsMenu"), require("../withOuia"), require("@patternfly/react-tokens"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.Dropdown, global.ToggleTemplate, global.pagination, global.reactStyles, global.Navigation, global.PaginationOptionsMenu, global.withOuia, global.reactTokens);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _Dropdown, _ToggleTemplate, _pagination, _reactStyles, _Navigation, _PaginationOptionsMenu, _withOuia, _reactTokens) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Pagination = exports.PaginationVariant = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _pagination2 = _interopRequireDefault(_pagination);

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

  let PaginationVariant = exports.PaginationVariant = undefined;

  (function (PaginationVariant) {
    PaginationVariant["top"] = "top";
    PaginationVariant["bottom"] = "bottom";
    PaginationVariant["left"] = "left";
    PaginationVariant["right"] = "right";
  })(PaginationVariant || (exports.PaginationVariant = PaginationVariant = {}));

  const defaultPerPageOptions = [{
    title: '10',
    value: 10
  }, {
    title: '20',
    value: 20
  }, {
    title: '50',
    value: 50
  }, {
    title: '100',
    value: 100
  }];

  const handleInputWidth = (lastPage, node) => {
    if (!node) {
      return;
    }

    const len = String(lastPage).length;

    if (len >= 3) {
      node.style.setProperty(_reactTokens.c_pagination__nav_page_select_c_form_control_width_chars.name, `${len}`);
    } else {
      node.style.setProperty(_reactTokens.c_pagination__nav_page_select_c_form_control_width_chars.name, '2');
    }
  };

  let paginationId = 0;

  class Pagination extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "paginationRef", React.createRef());
    }

    getLastPage() {
      const {
        itemCount,
        perPage
      } = this.props;
      return Math.ceil(itemCount / perPage) || 0;
    }

    componentDidMount() {
      const node = this.paginationRef.current;
      handleInputWidth(this.getLastPage(), node);
    }

    componentDidUpdate(prevProps) {
      const node = this.paginationRef.current;

      if (prevProps.perPage !== this.props.perPage || prevProps.itemCount !== this.props.itemCount) {
        handleInputWidth(this.getLastPage(), node);
      }
    }

    render() {
      const _this$props = this.props,
            {
        children,
        className,
        variant,
        isDisabled,
        isCompact,
        perPage,
        titles,
        firstPage,
        page: propPage,
        offset,
        defaultToFullPage,
        itemCount,
        itemsStart,
        itemsEnd,
        perPageOptions,
        dropDirection,
        widgetId,
        toggleTemplate,
        onSetPage,
        onPerPageSelect,
        onFirstClick,
        onPreviousClick,
        onNextClick,
        onPageInput,
        onLastClick,
        ouiaContext,
        ouiaId
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["children", "className", "variant", "isDisabled", "isCompact", "perPage", "titles", "firstPage", "page", "offset", "defaultToFullPage", "itemCount", "itemsStart", "itemsEnd", "perPageOptions", "dropDirection", "widgetId", "toggleTemplate", "onSetPage", "onPerPageSelect", "onFirstClick", "onPreviousClick", "onNextClick", "onPageInput", "onLastClick", "ouiaContext", "ouiaId"]);

      let page = propPage;

      if (!page && offset) {
        page = Math.ceil(offset / perPage);
      }

      const lastPage = this.getLastPage();

      if (page < firstPage && itemCount > 0) {
        page = firstPage;
      } else if (page > lastPage) {
        page = lastPage;
      }

      const firstIndex = itemCount <= 0 ? 0 : (page - 1) * perPage + 1;
      let lastIndex;

      if (itemCount <= 0) {
        lastIndex = 0;
      } else {
        lastIndex = page === lastPage ? itemCount : page * perPage;
      }

      return React.createElement("div", _extends({
        ref: this.paginationRef,
        className: (0, _reactStyles.css)(_pagination2.default.pagination, variant === PaginationVariant.bottom && _pagination2.default.modifiers.footer, isCompact && _pagination2.default.modifiers.compact, className),
        id: `${widgetId}-${paginationId++}`
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Pagination',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }, props), variant === PaginationVariant.top && React.createElement("div", {
        className: (0, _reactStyles.css)(_pagination2.default.paginationTotalItems)
      }, React.createElement(_ToggleTemplate.ToggleTemplate, {
        firstIndex: firstIndex,
        lastIndex: lastIndex,
        itemCount: itemCount,
        itemsTitle: titles.items
      })), React.createElement(_PaginationOptionsMenu.PaginationOptionsMenu, {
        itemsPerPageTitle: titles.itemsPerPage,
        perPageSuffix: titles.perPageSuffix,
        itemsTitle: isCompact ? '' : titles.items,
        optionsToggle: titles.optionsToggle,
        perPageOptions: perPageOptions,
        firstIndex: itemsStart !== null ? itemsStart : firstIndex,
        lastIndex: itemsEnd !== null ? itemsEnd : lastIndex,
        defaultToFullPage: defaultToFullPage,
        itemCount: itemCount,
        page: page,
        perPage: perPage,
        lastPage: lastPage,
        onPerPageSelect: onPerPageSelect,
        dropDirection: dropDirection,
        widgetId: widgetId,
        toggleTemplate: toggleTemplate,
        isDisabled: isDisabled
      }), React.createElement(_Navigation.Navigation, {
        pagesTitle: titles.page,
        toLastPage: titles.toLastPage,
        toPreviousPage: titles.toPreviousPage,
        toNextPage: titles.toNextPage,
        toFirstPage: titles.toFirstPage,
        currPage: titles.currPage,
        paginationTitle: titles.paginationTitle,
        page: itemCount <= 0 ? 0 : page,
        perPage: perPage,
        firstPage: itemsStart !== null ? itemsStart : 1,
        lastPage: lastPage,
        onSetPage: onSetPage,
        onFirstClick: onFirstClick,
        onPreviousClick: onPreviousClick,
        onNextClick: onNextClick,
        onLastClick: onLastClick,
        onPageInput: onPageInput,
        isDisabled: isDisabled,
        isCompact: isCompact
      }), children);
    }

  }

  _defineProperty(Pagination, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    itemCount: _propTypes2.default.number.isRequired,
    variant: _propTypes2.default.oneOf(['top', 'bottom', 'left', 'right']),
    isDisabled: _propTypes2.default.bool,
    isCompact: _propTypes2.default.bool,
    perPage: _propTypes2.default.number,
    perPageOptions: _propTypes2.default.arrayOf(_propTypes2.default.shape({
      title: _propTypes2.default.string,
      value: _propTypes2.default.number
    })),
    defaultToFullPage: _propTypes2.default.bool,
    firstPage: _propTypes2.default.number,
    page: _propTypes2.default.number,
    offset: _propTypes2.default.number,
    itemsStart: _propTypes2.default.number,
    itemsEnd: _propTypes2.default.number,
    widgetId: _propTypes2.default.string,
    dropDirection: _propTypes2.default.oneOf(['up', 'down']),
    titles: _propTypes2.default.shape({
      page: _propTypes2.default.string,
      items: _propTypes2.default.string,
      itemsPerPage: _propTypes2.default.string,
      perPageSuffix: _propTypes2.default.string,
      toFirstPage: _propTypes2.default.string,
      toPreviousPage: _propTypes2.default.string,
      toLastPage: _propTypes2.default.string,
      toNextPage: _propTypes2.default.string,
      optionsToggle: _propTypes2.default.string,
      currPage: _propTypes2.default.string,
      paginationTitle: _propTypes2.default.string
    }),
    toggleTemplate: _propTypes2.default.oneOfType([_propTypes2.default.func, _propTypes2.default.string]),
    onSetPage: _propTypes2.default.func,
    onFirstClick: _propTypes2.default.func,
    onPreviousClick: _propTypes2.default.func,
    onNextClick: _propTypes2.default.func,
    onLastClick: _propTypes2.default.func,
    onPageInput: _propTypes2.default.func,
    onPerPageSelect: _propTypes2.default.func
  });

  _defineProperty(Pagination, "defaultProps", {
    children: null,
    className: '',
    variant: PaginationVariant.top,
    isDisabled: false,
    isCompact: false,
    perPage: defaultPerPageOptions[0].value,
    titles: {
      items: '',
      page: '',
      itemsPerPage: 'Items per page',
      perPageSuffix: 'per page',
      toFirstPage: 'Go to first page',
      toPreviousPage: 'Go to previous page',
      toLastPage: 'Go to last page',
      toNextPage: 'Go to next page',
      optionsToggle: 'Items per page',
      currPage: 'Current page',
      paginationTitle: 'Pagination'
    },
    firstPage: 1,
    page: 0,
    offset: 0,
    defaultToFullPage: false,
    itemsStart: null,
    itemsEnd: null,
    perPageOptions: defaultPerPageOptions,
    dropDirection: _Dropdown.DropdownDirection.down,
    widgetId: 'pagination-options-menu',
    toggleTemplate: _ToggleTemplate.ToggleTemplate,
    onSetPage: () => undefined,
    onPerPageSelect: () => undefined,
    onFirstClick: () => undefined,
    onPreviousClick: () => undefined,
    onNextClick: () => undefined,
    onPageInput: () => undefined,
    onLastClick: () => undefined,
    ouiaContext: null,
    ouiaId: null
  });

  const PaginationWithOuiaContext = (0, _withOuia.withOuiaContext)(Pagination);
  exports.Pagination = PaginationWithOuiaContext;
});
//# sourceMappingURL=Pagination.js.map