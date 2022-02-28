import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface NavExpandableProps extends React.DetailedHTMLProps<React.LiHTMLAttributes<HTMLLIElement>, HTMLLIElement> {
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
    /** allow consumer to optionally override this callback and manage expand state externally */
    onExpand?: (e: React.MouseEvent<HTMLLIElement, MouseEvent>, val: boolean) => void;
}
interface NavExpandableState {
    expandedState: boolean;
}
export declare class NavExpandable extends React.Component<NavExpandableProps, NavExpandableState> {
    static defaultProps: PickOptional<NavExpandableProps>;
    id: string;
    state: {
        expandedState: boolean;
    };
    componentDidMount(): void;
    componentDidUpdate(prevProps: NavExpandableProps): void;
    onExpand: (e: React.MouseEvent<HTMLLIElement, MouseEvent>, val: boolean) => void;
    handleToggle: (e: React.MouseEvent<HTMLLIElement, MouseEvent>, onToggle: (event: React.MouseEvent<HTMLLIElement, MouseEvent>, groupId: React.ReactText, expandedState: boolean) => void) => void;
    render(): JSX.Element;
}
export {};
