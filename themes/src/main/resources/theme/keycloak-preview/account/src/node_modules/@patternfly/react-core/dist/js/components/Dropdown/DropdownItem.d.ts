import * as React from 'react';
import { InternalDropdownItemProps } from './InternalDropdownItem';
export interface DropdownItemProps extends InternalDropdownItemProps {
    /** Anything which can be rendered as dropdown item */
    children?: React.ReactNode;
    /** Classes applied to root element of dropdown item */
    className?: string;
    /** Class to be applied to list item */
    listItemClassName?: string;
    /** A ReactElement to render, or a string to use as the component tag.
     * Example: component={<Link to="/components/alert/">Alert</Link>}
     * Example: component="button"
     */
    component?: React.ReactNode;
    /** Variant of the item. The 'icon' variant should use DropdownItemIcon to wrap contained icons or images. */
    variant?: 'item' | 'icon';
    /** Render dropdown item as disabled option */
    isDisabled?: boolean;
    /** Forces display of the hover state of the element */
    isHovered?: boolean;
    /** Default hyperlink location */
    href?: string;
    /** Tooltip to display when hovered over the item */
    tooltip?: React.ReactNode;
    /** Additional tooltip props forwarded to the Tooltip component */
    tooltipProps?: any;
    /** Additional node to include alongside item within the <li> */
    additionalChild?: React.ReactNode;
    /** Custom item rendering that receives the DropdownContext */
    customChild?: React.ReactNode;
}
export declare const DropdownItem: React.FunctionComponent<DropdownItemProps>;
