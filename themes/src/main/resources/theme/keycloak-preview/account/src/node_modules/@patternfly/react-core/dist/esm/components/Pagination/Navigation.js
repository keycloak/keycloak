import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Pagination/pagination';
import { css } from '@patternfly/react-styles';
import AngleLeftIcon from '@patternfly/react-icons/dist/js/icons/angle-left-icon';
import AngleDoubleLeftIcon from '@patternfly/react-icons/dist/js/icons/angle-double-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import AngleDoubleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-double-right-icon';
import { Button, ButtonVariant } from '../Button';
import { pluralize } from '../../helpers';
import { KEY_CODES } from '../../helpers/constants';
export class Navigation extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleNewPage", (_evt, newPage) => {
      const {
        perPage,
        onSetPage
      } = this.props;
      const startIdx = (newPage - 1) * perPage;
      const endIdx = newPage * perPage;
      return onSetPage(_evt, newPage, perPage, startIdx, endIdx);
    });

    this.state = {
      userInputPage: this.props.page
    };
  }

  static parseInteger(input, lastPage) {
    // eslint-disable-next-line radix
    let inputPage = Number.parseInt(input, 10);

    if (!Number.isNaN(inputPage)) {
      inputPage = inputPage > lastPage ? lastPage : inputPage;
      inputPage = inputPage < 1 ? 1 : inputPage;
    }

    return inputPage;
  }

  onChange(event, lastPage) {
    const inputPage = Navigation.parseInteger(event.target.value, lastPage);
    this.setState({
      userInputPage: Number.isNaN(inputPage) ? event.target.value : inputPage
    });
  }

  onKeyDown(event, page, lastPage, onPageInput) {
    if (event.keyCode === KEY_CODES.ENTER) {
      const inputPage = Navigation.parseInteger(this.state.userInputPage, lastPage);
      onPageInput(event, Number.isNaN(inputPage) ? page : inputPage);
      this.handleNewPage(event, Number.isNaN(inputPage) ? page : inputPage);
    }
  }

  componentDidUpdate(lastState) {
    if (this.props.page !== lastState.page && this.props.page <= this.props.lastPage && this.state.userInputPage !== this.props.page) {
      this.setState({
        userInputPage: this.props.page
      });
    }
  }

  render() {
    const _this$props = this.props,
          {
      page,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      perPage,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onSetPage,
      isDisabled,
      lastPage,
      firstPage,
      pagesTitle,
      toLastPage,
      toNextPage,
      toFirstPage,
      toPreviousPage,
      currPage,
      paginationTitle,
      onNextClick,
      onPreviousClick,
      onFirstClick,
      onLastClick,
      onPageInput,
      className,
      isCompact
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["page", "perPage", "onSetPage", "isDisabled", "lastPage", "firstPage", "pagesTitle", "toLastPage", "toNextPage", "toFirstPage", "toPreviousPage", "currPage", "paginationTitle", "onNextClick", "onPreviousClick", "onFirstClick", "onLastClick", "onPageInput", "className", "isCompact"]);

    const {
      userInputPage
    } = this.state;
    return React.createElement("nav", _extends({
      className: css(styles.paginationNav, className),
      "aria-label": paginationTitle
    }, props), !isCompact && React.createElement(Button, {
      variant: ButtonVariant.plain,
      isDisabled: isDisabled || page === firstPage || page === 0,
      "aria-label": toFirstPage,
      "data-action": "first",
      onClick: event => {
        onFirstClick(event, 1);
        this.handleNewPage(event, 1);
        this.setState({
          userInputPage: 1
        });
      }
    }, React.createElement(AngleDoubleLeftIcon, null)), React.createElement(Button, {
      variant: ButtonVariant.plain,
      isDisabled: isDisabled || page === firstPage || page === 0,
      "data-action": "previous",
      onClick: event => {
        const newPage = page - 1 >= 1 ? page - 1 : 1;
        onPreviousClick(event, newPage);
        this.handleNewPage(event, newPage);
        this.setState({
          userInputPage: newPage
        });
      },
      "aria-label": toPreviousPage
    }, React.createElement(AngleLeftIcon, null)), !isCompact && React.createElement("div", {
      className: css(styles.paginationNavPageSelect)
    }, React.createElement("input", {
      className: css(styles.formControl),
      "aria-label": currPage,
      type: "number",
      disabled: isDisabled || page === firstPage && page === lastPage || page === 0,
      min: lastPage <= 0 && firstPage <= 0 ? 0 : 1,
      max: lastPage,
      value: userInputPage,
      onKeyDown: event => this.onKeyDown(event, page, lastPage, onPageInput),
      onChange: event => this.onChange(event, lastPage)
    }), React.createElement("span", {
      "aria-hidden": "true"
    }, "of ", pagesTitle ? pluralize(lastPage, pagesTitle) : lastPage)), React.createElement(Button, {
      variant: ButtonVariant.plain,
      isDisabled: isDisabled || page === lastPage,
      "aria-label": toNextPage,
      "data-action": "next",
      onClick: event => {
        const newPage = page + 1 <= lastPage ? page + 1 : lastPage;
        onNextClick(event, newPage);
        this.handleNewPage(event, newPage);
        this.setState({
          userInputPage: newPage
        });
      }
    }, React.createElement(AngleRightIcon, null)), !isCompact && React.createElement(Button, {
      variant: ButtonVariant.plain,
      isDisabled: isDisabled || page === lastPage,
      "aria-label": toLastPage,
      "data-action": "last",
      onClick: event => {
        onLastClick(event, lastPage);
        this.handleNewPage(event, lastPage);
        this.setState({
          userInputPage: lastPage
        });
      }
    }, React.createElement(AngleDoubleRightIcon, null)));
  }

}

_defineProperty(Navigation, "propTypes", {
  className: _pt.string,
  isDisabled: _pt.bool,
  isCompact: _pt.bool,
  lastPage: _pt.number,
  firstPage: _pt.number,
  pagesTitle: _pt.string,
  toLastPage: _pt.string,
  toPreviousPage: _pt.string,
  toNextPage: _pt.string,
  toFirstPage: _pt.string,
  currPage: _pt.string,
  paginationTitle: _pt.string,
  page: _pt.node.isRequired,
  perPage: _pt.number,
  onSetPage: _pt.any.isRequired,
  onNextClick: _pt.func,
  onPreviousClick: _pt.func,
  onFirstClick: _pt.func,
  onLastClick: _pt.func,
  onPageInput: _pt.func
});

_defineProperty(Navigation, "defaultProps", {
  className: '',
  isDisabled: false,
  isCompact: false,
  lastPage: 0,
  firstPage: 0,
  pagesTitle: '',
  toLastPage: 'Go to last page',
  toNextPage: 'Go to next page',
  toFirstPage: 'Go to first page',
  toPreviousPage: 'Go to previous page',
  currPage: 'Current page',
  paginationTitle: 'Pagination',
  onNextClick: () => undefined,
  onPreviousClick: () => undefined,
  onFirstClick: () => undefined,
  onLastClick: () => undefined,
  onPageInput: () => undefined
});
//# sourceMappingURL=Navigation.js.map