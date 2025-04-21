import * as React from 'react';
export interface HelperTextItemProps extends React.HTMLProps<HTMLDivElement | HTMLLIElement> {
    /** Content rendered inside the helper text item. */
    children?: React.ReactNode;
    /** Additional classes applied to the helper text item. */
    className?: string;
    /** Sets the component type of the helper text item. */
    component?: 'div' | 'li';
    /** Variant styling of the helper text item. */
    variant?: 'default' | 'indeterminate' | 'warning' | 'success' | 'error';
    /** Custom icon prefixing the helper text. This property will override the default icon paired with each helper text variant. */
    icon?: React.ReactNode;
    /** Flag indicating the helper text item is dynamic. This prop should be used when the
     * text content of the helper text item will never change, but the icon and styling will
     * be dynamically updated via the `variant` prop.
     */
    isDynamic?: boolean;
    /** Flag indicating the helper text should have an icon. Dynamic helper texts include icons by default while static helper texts do not. */
    hasIcon?: boolean;
    /** ID for the helper text item. The value of this prop can be passed into a form component's
     * aria-describedby prop when you intend for only specific helper text items to be announced to
     * assistive technologies.
     */
    id?: string;
    /** Text that is only accessible to screen readers in order to announce the status of a helper text item.
     * This prop can only be used when the isDynamic prop is also passed in.
     */
    screenReaderText?: string;
}
export declare const HelperTextItem: React.FunctionComponent<HelperTextItemProps>;
//# sourceMappingURL=HelperTextItem.d.ts.map