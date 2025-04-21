import * as React from 'react';
import { DropdownDirection, DropdownPosition } from '../Dropdown';
import { ToggleMenuBaseProps } from '../../helpers/Popper/Popper';
export interface ApplicationLauncherProps extends Omit<ToggleMenuBaseProps, 'menuAppendTo'>, React.HTMLProps<HTMLDivElement> {
    /** Additional element css classes */
    className?: string;
    /** Display menu above or below dropdown toggle */
    direction?: DropdownDirection | 'up' | 'down';
    /** Array of application launcher items */
    items?: React.ReactNode[];
    /** Render Application launcher toggle as disabled icon */
    isDisabled?: boolean;
    /** open bool */
    isOpen?: boolean;
    /** Indicates where menu will be alligned horizontally */
    position?: DropdownPosition | 'right' | 'left';
    /** Function callback called when user selects item */
    onSelect?: (event: any) => void;
    /** Callback called when application launcher toggle is clicked */
    onToggle?: (value: boolean) => void;
    /** Adds accessible text to the button. Required for plain buttons */
    'aria-label'?: string;
    /** Flag to indicate if application launcher has groups */
    isGrouped?: boolean;
    /** Toggle Icon, optional to override the icon used for the toggle */
    toggleIcon?: React.ReactNode;
    /** The container to append the menu to. Defaults to 'inline'.
     * If your menu is being cut off you can append it to an element higher up the DOM tree.
     * Some examples:
     * menuAppendTo="parent"
     * menuAppendTo={() => document.body}
     * menuAppendTo={document.getElementById('target')}
     */
    menuAppendTo?: HTMLElement | (() => HTMLElement) | 'inline' | 'parent';
    /** ID list of favorited ApplicationLauncherItems */
    favorites?: string[];
    /** Enables favorites. Callback called when an ApplicationLauncherItem's favorite button is clicked */
    onFavorite?: (itemId: string, isFavorite: boolean) => void;
    /** Enables search. Callback called when text input is entered into search box */
    onSearch?: (textInput: string) => void;
    /** Placeholder text for search input */
    searchPlaceholderText?: string;
    /** Text for search input when no results are found */
    searchNoResultsText?: string;
    /** Additional properties for search input */
    searchProps?: any;
    /** Label for the favorites group */
    favoritesLabel?: string;
    /** ID of toggle */
    toggleId?: string;
}
export declare class ApplicationLauncher extends React.Component<ApplicationLauncherProps> {
    static displayName: string;
    static defaultProps: ApplicationLauncherProps;
    createSearchBox: () => JSX.Element;
    render(): JSX.Element;
}
//# sourceMappingURL=ApplicationLauncher.d.ts.map