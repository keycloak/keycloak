import * as React from 'react';
import { NavContext } from './Nav';
export interface NavListProps extends React.DetailedHTMLProps<React.HTMLAttributes<HTMLUListElement>, HTMLUListElement> {
    /** Children nodes */
    children?: React.ReactNode;
    /** Additional classes added to the list */
    className?: string;
    /** Aria-label for the left scroll button */
    ariaLeftScroll?: string;
    /** Aria-label for the right scroll button */
    ariaRightScroll?: string;
}
export declare class NavList extends React.Component<NavListProps> {
    static displayName: string;
    static contextType: React.Context<import("./Nav").NavContextProps>;
    context: React.ContextType<typeof NavContext>;
    static defaultProps: NavListProps;
    state: {
        scrollViewAtStart: boolean;
        scrollViewAtEnd: boolean;
    };
    navList: React.RefObject<HTMLUListElement>;
    observer: any;
    handleScrollButtons: () => void;
    scrollLeft: () => void;
    scrollRight: () => void;
    componentDidMount(): void;
    componentWillUnmount(): void;
    render(): JSX.Element;
}
//# sourceMappingURL=NavList.d.ts.map