import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AppLauncher/app-launcher';
import { DropdownItem } from '../Dropdown';
import { ApplicationLauncherContent } from './ApplicationLauncherContent';
import { ApplicationLauncherContext } from './ApplicationLauncherContext';
import { ApplicationLauncherItemContext } from './ApplicationLauncherItemContext';
import StarIcon from '@patternfly/react-icons/dist/js/icons/star-icon';
export const ApplicationLauncherItem = (_ref) => {
  let {
    className = '',
    id,
    children,
    icon = null,
    isExternal = false,
    href,
    tooltip = null,
    tooltipProps = null,
    component = 'a',
    isFavorite = null,
    ariaIsFavoriteLabel = 'starred',
    ariaIsNotFavoriteLabel = 'not starred',
    customChild,
    enterTriggersArrowDown = false
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "id", "children", "icon", "isExternal", "href", "tooltip", "tooltipProps", "component", "isFavorite", "ariaIsFavoriteLabel", "ariaIsNotFavoriteLabel", "customChild", "enterTriggersArrowDown"]);

  return React.createElement(ApplicationLauncherItemContext.Provider, {
    value: {
      isExternal,
      icon
    }
  }, React.createElement(ApplicationLauncherContext.Consumer, null, ({
    onFavorite
  }) => React.createElement(DropdownItem, _extends({
    id: id,
    component: component,
    href: href || null,
    className: css(isExternal && styles.modifiers.external, isFavorite !== null && styles.modifiers.link, className),
    listItemClassName: css(onFavorite && styles.appLauncherMenuWrapper, isFavorite && styles.modifiers.favorite),
    tooltip: tooltip,
    tooltipProps: tooltipProps
  }, enterTriggersArrowDown === true && {
    enterTriggersArrowDown
  }, customChild && {
    customChild
  }, isFavorite !== null && {
    additionalChild: React.createElement("button", {
      className: css(styles.appLauncherMenuItem, styles.modifiers.action),
      "aria-label": isFavorite ? ariaIsFavoriteLabel : ariaIsNotFavoriteLabel,
      onClick: () => {
        onFavorite(id, isFavorite);
      }
    }, React.createElement(StarIcon, null))
  }, props), children && React.createElement(ApplicationLauncherContent, null, children))));
};
ApplicationLauncherItem.propTypes = {
  icon: _pt.node,
  isExternal: _pt.bool,
  tooltip: _pt.node,
  tooltipProps: _pt.any,
  component: _pt.node,
  isFavorite: _pt.bool,
  ariaIsFavoriteLabel: _pt.string,
  ariaIsNotFavoriteLabel: _pt.string,
  id: _pt.string,
  customChild: _pt.node,
  enterTriggersArrowDown: _pt.bool
};
//# sourceMappingURL=ApplicationLauncherItem.js.map