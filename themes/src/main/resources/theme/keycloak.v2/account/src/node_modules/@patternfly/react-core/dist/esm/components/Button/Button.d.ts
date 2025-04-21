import * as React from 'react';
import { OUIAProps } from '../../helpers';
export declare enum ButtonVariant {
    primary = "primary",
    secondary = "secondary",
    tertiary = "tertiary",
    danger = "danger",
    warning = "warning",
    link = "link",
    plain = "plain",
    control = "control"
}
export declare enum ButtonType {
    button = "button",
    submit = "submit",
    reset = "reset"
}
export interface ButtonProps extends Omit<React.HTMLProps<HTMLButtonElement>, 'ref'>, OUIAProps {
    /** Content rendered inside the button */
    children?: React.ReactNode;
    /** Additional classes added to the button */
    className?: string;
    /** Sets the base component to render. defaults to button */
    component?: React.ElementType<any> | React.ComponentType<any>;
    /** Adds active styling to button. */
    isActive?: boolean;
    /** Adds block styling to button */
    isBlock?: boolean;
    /** Adds disabled styling and disables the button using the disabled html attribute */
    isDisabled?: boolean;
    /** Adds disabled styling and communicates that the button is disabled using the aria-disabled html attribute */
    isAriaDisabled?: boolean;
    /** Adds progress styling to button */
    isLoading?: boolean;
    /** Text describing that current loading status or progress */
    spinnerAriaValueText?: string;
    /** Accessible label for the spinner to describe what is loading */
    spinnerAriaLabel?: string;
    /** Id of element which describes what is being loaded */
    spinnerAriaLabelledBy?: string;
    /** Events to prevent when the button is in an aria-disabled state */
    inoperableEvents?: string[];
    /** Adds inline styling to a link button */
    isInline?: boolean;
    /** Sets button type */
    type?: 'button' | 'submit' | 'reset';
    /** Adds button variant styles */
    variant?: 'primary' | 'secondary' | 'tertiary' | 'danger' | 'warning' | 'link' | 'plain' | 'control';
    /** Sets position of the link icon */
    iconPosition?: 'left' | 'right';
    /** Adds accessible text to the button. */
    'aria-label'?: string;
    /** Icon for the button. Usable by all variants except for plain. */
    icon?: React.ReactNode | null;
    /** Sets the button tabindex. */
    tabIndex?: number;
    /** Adds small styling to the button */
    isSmall?: boolean;
    /** Adds large styling to the button */
    isLarge?: boolean;
    /** Adds danger styling to secondary or link button variants */
    isDanger?: boolean;
    /** Forwarded ref */
    innerRef?: React.Ref<any>;
}
export declare const Button: React.ForwardRefExoticComponent<ButtonProps & React.RefAttributes<any>>;
//# sourceMappingURL=Button.d.ts.map