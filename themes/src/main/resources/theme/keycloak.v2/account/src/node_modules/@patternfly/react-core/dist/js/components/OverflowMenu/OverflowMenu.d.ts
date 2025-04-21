import * as React from 'react';
export interface OverflowMenuProps extends React.HTMLProps<HTMLDivElement> {
    /** Any elements that can be rendered in the menu */
    children?: any;
    /** Additional classes added to the OverflowMenu. */
    className?: string;
    /** Indicates breakpoint at which to switch between horizontal menu and vertical dropdown */
    breakpoint: 'md' | 'lg' | 'xl' | '2xl';
}
export interface OverflowMenuState extends React.HTMLProps<HTMLDivElement> {
    isBelowBreakpoint: boolean;
}
export declare class OverflowMenu extends React.Component<OverflowMenuProps, OverflowMenuState> {
    static displayName: string;
    constructor(props: OverflowMenuProps);
    componentDidMount(): void;
    componentWillUnmount(): void;
    handleResize: () => void;
    render(): JSX.Element;
}
//# sourceMappingURL=OverflowMenu.d.ts.map