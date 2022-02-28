import * as React from 'react';
export declare const variantIcons: {
    success: React.SFC<import("@patternfly/react-icons/dist/js/common").IconProps>;
    danger: React.SFC<import("@patternfly/react-icons/dist/js/common").IconProps>;
    warning: React.SFC<import("@patternfly/react-icons/dist/js/common").IconProps>;
    info: React.SFC<import("@patternfly/react-icons/dist/js/common").IconProps>;
    default: React.SFC<import("@patternfly/react-icons/dist/js/common").IconProps>;
};
export interface AlertIconProps extends React.HTMLProps<HTMLDivElement> {
    /** variant */
    variant: 'success' | 'danger' | 'warning' | 'info' | 'default';
    /** className */
    className?: string;
}
export declare const AlertIcon: ({ variant, className, ...props }: AlertIconProps) => JSX.Element;
