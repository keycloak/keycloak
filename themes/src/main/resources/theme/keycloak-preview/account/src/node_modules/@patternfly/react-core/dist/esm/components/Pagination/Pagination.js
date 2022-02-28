import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { DropdownDirection } from '../Dropdown';
import { ToggleTemplate } from './ToggleTemplate';
import styles from '@patternfly/react-styles/css/components/Pagination/pagination';
import { css } from '@patternfly/react-styles';
import { Navigation } from './Navigation';
import { PaginationOptionsMenu } from './PaginationOptionsMenu';
import { withOuiaContext } from '../withOuia';
import { c_pagination__nav_page_select_c_form_control_width_chars as widthChars } from '@patternfly/react-tokens';
export let PaginationVariant;

(function (PaginationVariant) {
  PaginationVariant["top"] = "top";
  PaginationVariant["bottom"] = "bottom";
  PaginationVariant["left"] = "left";
  PaginationVariant["right"] = "right";
})(PaginationVariant || (PaginationVariant = {}));

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
    node.style.setProperty(widthChars.name, `${len}`);
  } else {
    node.style.setProperty(widthChars.name, '2');
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
      className: css(styles.pagination, variant === PaginationVariant.bottom && styles.modifiers.footer, isCompact && styles.modifiers.compact, className),
      id: `${widgetId}-${paginationId++}`
    }, ouiaContext.isOuia && {
      'data-ouia-component-type': 'Pagination',
      'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
    }, props), variant === PaginationVariant.top && React.createElement("div", {
      className: css(styles.paginationTotalItems)
    }, React.createElement(ToggleTemplate, {
      firstIndex: firstIndex,
      lastIndex: lastIndex,
      itemCount: itemCount,
      itemsTitle: titles.items
    })), React.createElement(PaginationOptionsMenu, {
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
    }), React.createElement(Navigation, {
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
  children: _pt.node,
  className: _pt.string,
  itemCount: _pt.number.isRequired,
  variant: _pt.oneOf(['top', 'bottom', 'left', 'right']),
  isDisabled: _pt.bool,
  isCompact: _pt.bool,
  perPage: _pt.number,
  perPageOptions: _pt.arrayOf(_pt.shape({
    title: _pt.string,
    value: _pt.number
  })),
  defaultToFullPage: _pt.bool,
  firstPage: _pt.number,
  page: _pt.number,
  offset: _pt.number,
  itemsStart: _pt.number,
  itemsEnd: _pt.number,
  widgetId: _pt.string,
  dropDirection: _pt.oneOf(['up', 'down']),
  titles: _pt.shape({
    page: _pt.string,
    items: _pt.string,
    itemsPerPage: _pt.string,
    perPageSuffix: _pt.string,
    toFirstPage: _pt.string,
    toPreviousPage: _pt.string,
    toLastPage: _pt.string,
    toNextPage: _pt.string,
    optionsToggle: _pt.string,
    currPage: _pt.string,
    paginationTitle: _pt.string
  }),
  toggleTemplate: _pt.oneOfType([_pt.func, _pt.string]),
  onSetPage: _pt.func,
  onFirstClick: _pt.func,
  onPreviousClick: _pt.func,
  onNextClick: _pt.func,
  onLastClick: _pt.func,
  onPageInput: _pt.func,
  onPerPageSelect: _pt.func
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
  dropDirection: DropdownDirection.down,
  widgetId: 'pagination-options-menu',
  toggleTemplate: ToggleTemplate,
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

const PaginationWithOuiaContext = withOuiaContext(Pagination);
export { PaginationWithOuiaContext as Pagination };
//# sourceMappingURL=Pagination.js.map