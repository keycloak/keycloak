import * as React from 'react';
export interface JumpLinksProps extends Omit<React.HTMLProps<HTMLElement>, 'label'> {
    /** Whether to center children. */
    isCentered?: boolean;
    /** Whether the layout of children is vertical or horizontal. */
    isVertical?: boolean;
    /** Label to add to nav element. */
    label?: React.ReactNode;
    /** Flag to always show the label when using `expandable` */
    alwaysShowLabel?: boolean;
    /** Aria-label to add to nav element. Defaults to label. */
    'aria-label'?: string;
    /** Selector for the scrollable element to spy on. Not passing a selector disables spying. */
    scrollableSelector?: string;
    /** The index of the child Jump link to make active. */
    activeIndex?: number;
    /** Children nodes */
    children?: React.ReactNode;
    /** Offset to add to `scrollPosition`, potentially for a masthead which content scrolls under. */
    offset?: number;
    /** When to collapse/expand at different breakpoints */
    expandable?: {
        default?: 'expandable' | 'nonExpandable';
        sm?: 'expandable' | 'nonExpandable';
        md?: 'expandable' | 'nonExpandable';
        lg?: 'expandable' | 'nonExpandable';
        xl?: 'expandable' | 'nonExpandable';
        '2xl'?: 'expandable' | 'nonExpandable';
    };
    /** On mobile whether or not the JumpLinks starts out expanded */
    isExpanded?: boolean;
    /** Aria label for expandable toggle */
    toggleAriaLabel?: string;
    /** Class for nav */
    className?: string;
}
export declare const JumpLinks: React.FunctionComponent<JumpLinksProps>;
//# sourceMappingURL=JumpLinks.d.ts.map