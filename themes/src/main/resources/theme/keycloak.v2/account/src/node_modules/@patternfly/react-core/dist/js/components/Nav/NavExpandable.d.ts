import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
import { OUIAProps } from '../../helpers';
export interface NavExpandableProps extends React.DetailedHTMLProps<React.LiHTMLAttributes<HTMLLIElement>, HTMLLIElement>, OUIAProps {
    /** Title shown for the expandable list */
    title: string;
    /** If defined, screen readers will read this text instead of the list title */
    srText?: string;
    /** Boolean to programatically expand or collapse section */
    isExpanded?: boolean;
    /** Anything that can be rendered inside of the expandable list */
    children?: React.ReactNode;
    /** Additional classes added to the container */
    className?: string;
    /** Group identifier, will be returned with the onToggle and onSelect callback passed to the Nav component */
    groupId?: string | number;
    /** If true makes the expandable list title active */
    isActive?: boolean;
    /** Identifier to use for the section aria label */
    id?: string;
    /** allow consumer to optionally override this callback and manage expand state externally. if passed will not call Nav's onToggle. */
    onExpand?: (e: React.MouseEvent<HTMLButtonElement, MouseEvent>, val: boolean) => void;
    /** Additional props added to the NavExpandable <button> */
    buttonProps?: any;
}
interface NavExpandableState {
    expandedState: boolean;
    ouiaStateId: string;
}
export declare class NavExpandable extends React.Component<NavExpandableProps, NavExpandableState> {
    static displayName: string;
    static defaultProps: PickOptional<NavExpandableProps>;
    id: string;
    state: {
        expandedState: boolean;
        ouiaStateId: string;
    };
    componentDidMount(): void;
    componentDidUpdate(prevProps: NavExpandableProps): void;
    onExpand: (e: React.MouseEvent<HTMLButtonElement, MouseEvent>, onToggle: (event: React.MouseEvent<HTMLButtonElement, MouseEvent>, groupId: string | number, expandedState: boolean) => void) => void;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=NavExpandable.d.ts.map