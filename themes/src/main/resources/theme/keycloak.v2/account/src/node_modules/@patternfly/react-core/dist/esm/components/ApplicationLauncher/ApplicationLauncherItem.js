import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AppLauncher/app-launcher';
import { DropdownItem } from '../Dropdown';
import { ApplicationLauncherContent } from './ApplicationLauncherContent';
import { ApplicationLauncherContext } from './ApplicationLauncherContext';
import { ApplicationLauncherItemContext } from './ApplicationLauncherItemContext';
import StarIcon from '@patternfly/react-icons/dist/esm/icons/star-icon';
import { getUniqueId } from '../../helpers/util';
export const ApplicationLauncherItem = (_a) => {
    var { className = '', id, children, icon = null, isExternal = false, href, tooltip = null, tooltipProps = null, component = 'a', isFavorite = null, ariaIsFavoriteLabel = 'starred', ariaIsNotFavoriteLabel = 'not starred', customChild, enterTriggersArrowDown = false } = _a, props = __rest(_a, ["className", "id", "children", "icon", "isExternal", "href", "tooltip", "tooltipProps", "component", "isFavorite", "ariaIsFavoriteLabel", "ariaIsNotFavoriteLabel", "customChild", "enterTriggersArrowDown"]);
    return (React.createElement(ApplicationLauncherItemContext.Provider, { value: { isExternal, icon } },
        React.createElement(ApplicationLauncherContext.Consumer, null, ({ onFavorite }) => (React.createElement(DropdownItem, Object.assign({ id: id, component: component, href: href || null, className: css(isExternal && styles.modifiers.external, isFavorite !== null && styles.modifiers.link, className), listItemClassName: css(onFavorite && styles.appLauncherMenuWrapper, isFavorite && styles.modifiers.favorite), tooltip: tooltip, tooltipProps: tooltipProps }, (enterTriggersArrowDown === true && { enterTriggersArrowDown }), (customChild && { customChild }), (isFavorite !== null && {
            additionalChild: (React.createElement("button", { className: css(styles.appLauncherMenuItem, styles.modifiers.action), "aria-label": isFavorite ? ariaIsFavoriteLabel : ariaIsNotFavoriteLabel, onClick: () => {
                    onFavorite((id || getUniqueId('app-launcher-option')).replace('favorite-', ''), isFavorite);
                } },
                React.createElement(StarIcon, null)))
        }), props), children && React.createElement(ApplicationLauncherContent, null, children))))));
};
ApplicationLauncherItem.displayName = 'ApplicationLauncherItem';
//# sourceMappingURL=ApplicationLauncherItem.js.map