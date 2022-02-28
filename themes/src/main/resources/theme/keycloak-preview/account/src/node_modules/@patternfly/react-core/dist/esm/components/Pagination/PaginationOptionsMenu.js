import _pt from "prop-types";

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import paginationStyles from '@patternfly/react-styles/css/components/Pagination/pagination';
import { css } from '@patternfly/react-styles';
import { DropdownItem, DropdownDirection, DropdownWithContext, DropdownContext } from '../Dropdown';
import CheckIcon from '@patternfly/react-icons/dist/js/icons/check-icon';
import { OptionsToggle } from './OptionsToggle';
export class PaginationOptionsMenu extends React.Component {
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
      }) => React.createElement(DropdownItem, {
        key: value,
        component: "button",
        "data-action": `per-page-${value}`,
        className: css(perPage === value && 'pf-m-selected'),
        onClick: event => this.handleNewPerPage(event, value)
      }, title, React.createElement("span", {
        className: css(paginationStyles.paginationMenuText)
      }, ` ${perPageSuffix}`), perPage === value && React.createElement("i", {
        className: css(styles.optionsMenuMenuItemIcon)
      }, React.createElement(CheckIcon, null))));
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
    return React.createElement(DropdownContext.Provider, {
      value: {
        id: widgetId,
        onSelect: this.onSelect,
        toggleIconClass: styles.optionsMenuToggleIcon,
        toggleTextClass: styles.optionsMenuToggleText,
        menuClass: styles.optionsMenuMenu,
        itemClass: styles.optionsMenuMenuItem,
        toggleClass: ' ',
        baseClass: styles.optionsMenu,
        disabledClass: styles.modifiers.disabled,
        menuComponent: 'ul',
        baseComponent: 'div'
      }
    }, React.createElement(DropdownWithContext, {
      direction: dropDirection,
      isOpen: isOpen,
      toggle: React.createElement(OptionsToggle, {
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

_defineProperty(PaginationOptionsMenu, "propTypes", {
  className: _pt.string,
  widgetId: _pt.string,
  isDisabled: _pt.bool,
  dropDirection: _pt.oneOf(['up', 'down']),
  perPageOptions: _pt.arrayOf(_pt.any),
  itemsPerPageTitle: _pt.string,
  page: _pt.number,
  perPageSuffix: _pt.string,
  itemsTitle: _pt.string,
  optionsToggle: _pt.string,
  itemCount: _pt.number,
  firstIndex: _pt.number,
  lastIndex: _pt.number,
  defaultToFullPage: _pt.bool,
  perPage: _pt.number,
  lastPage: _pt.number,
  toggleTemplate: _pt.oneOfType([_pt.func, _pt.string]),
  onPerPageSelect: _pt.any
});

_defineProperty(PaginationOptionsMenu, "defaultProps", {
  className: '',
  widgetId: '',
  isDisabled: false,
  dropDirection: DropdownDirection.down,
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
//# sourceMappingURL=PaginationOptionsMenu.js.map