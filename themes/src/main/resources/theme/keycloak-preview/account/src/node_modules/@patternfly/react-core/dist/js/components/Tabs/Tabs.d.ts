import * as React from 'react';
import { InjectedOuiaProps } from '../withOuia';
export declare enum TabsVariant {
    div = "div",
    nav = "nav"
}
export interface TabsProps extends Omit<React.HTMLProps<HTMLElement | HTMLDivElement>, 'onSelect'> {
    /** content rendered inside the Tabs Component. */
    children: React.ReactNode;
    /** additional classes added to the Tabs */
    className?: string;
    /** the index of the active tab */
    activeKey?: number | string;
    /** handle tab selection */
    onSelect?: (event: React.MouseEvent<HTMLElement, MouseEvent>, eventKey: number | string) => void;
    /** uniquely identifies the Tabs */
    id?: string;
    /** enables the filled tab list layout */
    isFilled?: boolean;
    /** enables Secondary Tab styling */
    isSecondary?: boolean;
    /** aria-label for the left Scroll Button */
    leftScrollAriaLabel?: string;
    /** aria-label for the right Scroll Button */
    rightScrollAriaLabel?: string;
    /** determines what tag is used around the Tabs. Use "nav" to define the Tabs inside a navigation region */
    variant?: 'div' | 'nav';
    /** provides an accessible label for the Tabs. Labels should be unique for each set of Tabs that are present on a page. When variant is set to nav, this prop should be defined to differentiate the Tabs from other navigation regions on the page. */
    'aria-label'?: string;
    /** waits until the first "enter" transition to mount tab children (add them to the DOM) */
    mountOnEnter?: boolean;
    /** unmounts tab children (removes them from the DOM) when they are no longer visible */
    unmountOnExit?: boolean;
}
export interface TabsState {
    showLeftScrollButton: boolean;
    showRightScrollButton: boolean;
    highlightLeftScrollButton: boolean;
    highlightRightScrollButton: boolean;
    shownKeys: (string | number)[];
}
declare const TabsWithOuiaContext: React.FunctionComponent<TabsProps & InjectedOuiaProps>;
export { TabsWithOuiaContext as Tabs };
