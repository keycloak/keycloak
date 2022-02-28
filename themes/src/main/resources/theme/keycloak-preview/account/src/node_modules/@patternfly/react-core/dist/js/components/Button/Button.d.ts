import * as React from 'react';
import { InjectedOuiaProps } from '../withOuia';
export declare enum ButtonVariant {
    primary = "primary",
    secondary = "secondary",
    tertiary = "tertiary",
    danger = "danger",
    link = "link",
    plain = "plain",
    control = "control"
}
export declare enum ButtonType {
    button = "button",
    submit = "submit",
    reset = "reset"
}
export interface ButtonProps extends React.HTMLProps<HTMLButtonElement> {
    /** Content rendered inside the button */
    children?: React.ReactNode;
    /** Additional classes added to the button */
    className?: string;
    /** Sets the base component to render. defaults to button */
    component?: React.ElementType<any>;
    /** Adds active styling to button. */
    isActive?: boolean;
    /** Adds block styling to button */
    isBlock?: boolean;
    /** Disables the button and adds disabled styling */
    isDisabled?: boolean;
    /** Adds focus styling to the button */
    isFocus?: boolean;
    /** Adds hover styling to the button */
    isHover?: boolean;
    /** Adds inline styling to a link button */
    isInline?: boolean;
    /** Sets button type */
    type?: 'button' | 'submit' | 'reset';
    /** Adds button variant styles */
    variant?: 'primary' | 'secondary' | 'tertiary' | 'danger' | 'link' | 'plain' | 'control';
    /** Sets position of the link icon */
    iconPosition?: 'left' | 'right';
    /** Adds accessible text to the button. */
    'aria-label'?: string;
    /** Icon for the button if variant is a link */
    icon?: React.ReactNode | null;
    /** Set button tab index unless component is not a button and is disabled */
    tabIndex?: number;
}
declare const ButtonWithOuiaContext: React.FunctionComponent<ButtonProps & InjectedOuiaProps>;
export { ButtonWithOuiaContext as Button };
