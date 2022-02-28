import * as React from 'react';
import { InjectedOuiaProps } from '../withOuia';
export declare type NavSelectClickHandler = (e: React.FormEvent<HTMLInputElement>, itemId: number | string, groupId: number | string, to: string) => void;
export interface NavProps extends Omit<React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement>, 'onSelect'> {
    /** Anything that can be rendered inside of the nav */
    children?: React.ReactNode;
    /** Additional classes added to the container */
    className?: string;
    /** Callback for updating when item selection changes */
    onSelect?: (selectedItem: {
        groupId: number | string;
        itemId: number | string;
        to: string;
        event: React.FormEvent<HTMLInputElement>;
    }) => void;
    /** Callback for when a list is expanded or collapsed */
    onToggle?: (toggledItem: {
        groupId: number | string;
        isExpanded: boolean;
        event: React.FormEvent<HTMLInputElement>;
    }) => void;
    /** Accessibility label */
    'aria-label'?: string;
    /** Indicates which theme color to use */
    theme?: 'dark' | 'light';
}
export declare const NavContext: React.Context<{}>;
declare const NavWithOuiaContext: React.FunctionComponent<NavProps & InjectedOuiaProps>;
export { NavWithOuiaContext as Nav };
