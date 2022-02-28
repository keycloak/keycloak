import _pt from "prop-types";

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AppLauncher/app-launcher';
import formStyles from '@patternfly/react-styles/css/components/FormControl/form-control';
import ThIcon from '@patternfly/react-icons/dist/js/icons/th-icon';
import { DropdownDirection, DropdownPosition, DropdownToggle, DropdownContext } from '../Dropdown';
import { DropdownWithContext } from '../Dropdown/DropdownWithContext';
import { ApplicationLauncherGroup } from './ApplicationLauncherGroup';
import { ApplicationLauncherSeparator } from './ApplicationLauncherSeparator';
import { ApplicationLauncherItem } from './ApplicationLauncherItem';
import { ApplicationLauncherContext } from './ApplicationLauncherContext';
export class ApplicationLauncher extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "createSearchBox", () => {
      const {
        onSearch,
        searchPlaceholderText,
        searchProps
      } = this.props;
      return React.createElement("div", {
        key: "search",
        className: css(styles.appLauncherMenuSearch)
      }, React.createElement(ApplicationLauncherItem, {
        customChild: React.createElement("input", _extends({
          type: "search",
          className: css(formStyles.formControl),
          placeholder: searchPlaceholderText,
          onChange: e => onSearch(e.target.value)
        }, searchProps))
      }));
    });

    _defineProperty(this, "createRenderableFavorites", () => {
      const {
        items,
        isGrouped,
        favorites
      } = this.props;

      if (isGrouped) {
        const favoriteItems = [];
        items.forEach(group => group.props.children.filter(item => favorites.includes(item.props.id)).map(item => favoriteItems.push(React.cloneElement(item, {
          isFavorite: true,
          enterTriggersArrowDown: true
        }))));
        return favoriteItems;
      }

      return items.filter(item => favorites.includes(item.props.id)).map(item => React.cloneElement(item, {
        isFavorite: true,
        enterTriggersArrowDown: true
      }));
    });

    _defineProperty(this, "extendItemsWithFavorite", () => {
      const {
        items,
        isGrouped,
        favorites
      } = this.props;

      if (isGrouped) {
        return items.map(group => React.cloneElement(group, {
          children: React.Children.map(group.props.children, item => {
            if (item.type === ApplicationLauncherSeparator) {
              return item;
            }

            return React.cloneElement(item, {
              isFavorite: favorites.some(favoriteId => favoriteId === item.props.id)
            });
          })
        }));
      }

      return items.map(item => React.cloneElement(item, {
        isFavorite: favorites.some(favoriteId => favoriteId === item.props.id)
      }));
    });
  }

  render() {
    const _this$props = this.props,
          {
      'aria-label': ariaLabel,
      isOpen,
      onToggle,
      toggleIcon,
      toggleId,
      onSelect,
      isDisabled,
      className,
      isGrouped,
      dropdownItems,
      favorites,
      onFavorite,
      onSearch,
      items,

      /* eslint-disable @typescript-eslint/no-unused-vars */
      searchPlaceholderText,
      searchProps,
      ref,

      /* eslint-enable @typescript-eslint/no-unused-vars */
      favoritesLabel,
      searchNoResultsText
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["aria-label", "isOpen", "onToggle", "toggleIcon", "toggleId", "onSelect", "isDisabled", "className", "isGrouped", "dropdownItems", "favorites", "onFavorite", "onSearch", "items", "searchPlaceholderText", "searchProps", "ref", "favoritesLabel", "searchNoResultsText"]);

    let renderableItems = [];

    if (onFavorite) {
      let favoritesGroup = [];
      let renderableFavorites = [];

      if (favorites.length > 0) {
        renderableFavorites = this.createRenderableFavorites();
        favoritesGroup = [React.createElement(ApplicationLauncherGroup, {
          key: "favorites",
          label: favoritesLabel
        }, renderableFavorites, React.createElement(ApplicationLauncherSeparator, {
          key: "separator"
        }))];
      }

      if (renderableFavorites.length > 0) {
        renderableItems = favoritesGroup.concat(this.extendItemsWithFavorite());
      } else {
        renderableItems = this.extendItemsWithFavorite();
      }
    } else {
      renderableItems = items;
    }

    if (items.length === 0 && dropdownItems.length === 0) {
      renderableItems = [React.createElement(ApplicationLauncherGroup, {
        key: "no-results-group"
      }, React.createElement(ApplicationLauncherItem, {
        key: "no-results"
      }, searchNoResultsText))];
    }

    if (onSearch) {
      renderableItems = [this.createSearchBox(), ...renderableItems];
    }

    return React.createElement(ApplicationLauncherContext.Provider, {
      value: {
        onFavorite
      }
    }, React.createElement(DropdownContext.Provider, {
      value: {
        onSelect,
        menuClass: styles.appLauncherMenu,
        itemClass: styles.appLauncherMenuItem,
        toggleClass: styles.appLauncherToggle,
        baseClass: styles.appLauncher,
        baseComponent: 'nav',
        sectionClass: styles.appLauncherGroup,
        sectionTitleClass: styles.appLauncherGroupTitle,
        sectionComponent: 'section',
        disabledClass: styles.modifiers.disabled,
        hoverClass: styles.modifiers.hover,
        separatorClass: styles.appLauncherSeparator
      }
    }, React.createElement(DropdownWithContext, _extends({}, props, {
      dropdownItems: renderableItems.length ? renderableItems : dropdownItems,
      isOpen: isOpen,
      className: className,
      "aria-label": ariaLabel,
      toggle: React.createElement(DropdownToggle, {
        id: toggleId,
        iconComponent: null,
        isOpen: isOpen,
        onToggle: onToggle,
        isDisabled: isDisabled,
        "aria-label": ariaLabel
      }, toggleIcon),
      isGrouped: isGrouped
    }))));
  }

}

_defineProperty(ApplicationLauncher, "propTypes", {
  className: _pt.string,
  direction: _pt.oneOfType([_pt.any, _pt.oneOf(['up']), _pt.oneOf(['down'])]),
  dropdownItems: _pt.arrayOf(_pt.node),
  items: _pt.arrayOf(_pt.node),
  isDisabled: _pt.bool,
  isOpen: _pt.bool,
  position: _pt.oneOfType([_pt.any, _pt.oneOf(['right']), _pt.oneOf(['left'])]),
  onSelect: _pt.func,
  onToggle: _pt.func,
  'aria-label': _pt.string,
  isGrouped: _pt.bool,
  toggleIcon: _pt.node,
  favorites: _pt.arrayOf(_pt.string),
  onFavorite: _pt.func,
  onSearch: _pt.func,
  searchPlaceholderText: _pt.string,
  searchNoResultsText: _pt.string,
  searchProps: _pt.any,
  favoritesLabel: _pt.string,
  toggleId: _pt.string
});

_defineProperty(ApplicationLauncher, "defaultProps", {
  className: '',
  isDisabled: false,
  direction: DropdownDirection.down,
  dropdownItems: [],
  favorites: [],
  items: [],
  isOpen: false,
  position: DropdownPosition.left,

  /* eslint-disable @typescript-eslint/no-unused-vars */
  onSelect: _event => undefined,
  onToggle: _value => undefined,

  /* eslint-enable @typescript-eslint/no-unused-vars */
  'aria-label': 'Application launcher',
  isGrouped: false,
  toggleIcon: React.createElement(ThIcon, null),
  searchPlaceholderText: 'Filter by name...',
  searchNoResultsText: 'No results found',
  favoritesLabel: 'Favorites'
});
//# sourceMappingURL=ApplicationLauncher.js.map