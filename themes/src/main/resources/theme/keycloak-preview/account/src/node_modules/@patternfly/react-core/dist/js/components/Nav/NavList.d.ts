import * as React from 'react';
export interface NavListProps extends React.DetailedHTMLProps<React.HTMLAttributes<HTMLUListElement>, HTMLUListElement> {
    /** Children nodes */
    children?: React.ReactNode;
    /** Additional classes added to the list */
    className?: string;
    /** Indicates the list type. */
    variant?: 'default' | 'simple' | 'horizontal' | 'tertiary';
    /** aria-label for the left scroll button */
    ariaLeftScroll?: string;
    /** aria-label for the right scroll button */
    ariaRightScroll?: string;
}
export declare class NavList extends React.Component<NavListProps> {
    static contextType: React.Context<{}>;
    static defaultProps: NavListProps;
    navList: React.RefObject<HTMLUListElement>;
    handleScrollButtons: () => void;
    scrollLeft: () => void;
    scrollRight: () => void;
    componentDidMount(): void;
    componentWillUnmount(): void;
    render(): JSX.Element;
}
