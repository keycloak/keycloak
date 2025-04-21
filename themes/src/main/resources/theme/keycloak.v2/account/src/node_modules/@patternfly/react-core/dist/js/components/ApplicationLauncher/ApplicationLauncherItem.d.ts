import * as React from 'react';
import { DropdownItemProps } from '../Dropdown';
export interface ApplicationLauncherItemProps {
    /** Icon rendered before the text */
    icon?: React.ReactNode;
    /** If clicking on the item should open the page in a separate window */
    isExternal?: boolean;
    /** Tooltip to display when hovered over the item */
    tooltip?: React.ReactNode;
    /** Additional tooltip props forwarded to the Tooltip component */
    tooltipProps?: any;
    /** A ReactElement to render, or a string to use as the component tag.
     * Example: component={<Link to="/components/alert/">Alert</Link>}
     * Example: component="button"
     */
    component?: React.ReactNode;
    /** Flag indicating if the item is favorited */
    isFavorite?: boolean;
    /** Aria label text for favoritable button when favorited */
    ariaIsFavoriteLabel?: string;
    /** Aria label text for favoritable button when not favorited */
    ariaIsNotFavoriteLabel?: string;
    /** ID of the item. Required for tracking favorites. */
    id?: string;
    customChild?: React.ReactNode;
    /** Flag indicating if hitting enter triggers an arrow down key press. Automatically passed to favorites list items. */
    enterTriggersArrowDown?: boolean;
}
export declare const ApplicationLauncherItem: React.FunctionComponent<ApplicationLauncherItemProps & DropdownItemProps>;
//# sourceMappingURL=ApplicationLauncherItem.d.ts.map