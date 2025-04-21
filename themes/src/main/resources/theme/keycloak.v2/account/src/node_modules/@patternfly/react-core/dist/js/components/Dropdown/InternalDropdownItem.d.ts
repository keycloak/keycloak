import * as React from 'react';
export interface InternalDropdownItemProps extends React.HTMLProps<HTMLAnchorElement> {
    /** Anything which can be rendered as dropdown item */
    children?: React.ReactNode;
    /** Whether to set className on component when React.isValidElement(component) */
    styleChildren?: boolean;
    /** Classes applied to root element of dropdown item */
    className?: string;
    /** Class applied to list element */
    listItemClassName?: string;
    /** Indicates which component will be used as dropdown item. Will have className injected if React.isValidElement(component) */
    component?: React.ReactNode;
    /** Role for the item */
    role?: string;
    /** Render dropdown item as disabled option */
    isDisabled?: boolean;
    /** Render dropdown item as aria disabled option */
    isAriaDisabled?: boolean;
    /** Render dropdown item as a non-interactive item */
    isPlainText?: boolean;
    /** Forces display of the hover state of the element */
    isHovered?: boolean;
    /** Default hyperlink location */
    href?: string;
    /** Tooltip to display when hovered over the item */
    tooltip?: React.ReactNode;
    /** Additional tooltip props forwarded to the Tooltip component */
    tooltipProps?: any;
    index?: number;
    context?: {
        keyHandler?: (index: number, innerIndex: number, direction: string) => void;
        sendRef?: (index: number, ref: any, isDisabled: boolean, isSeparator: boolean) => void;
    };
    /** Callback for click event */
    onClick?: (event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent) => void;
    /** ID for the list element */
    id?: string;
    /** ID for the component element */
    componentID?: string;
    /** Additional content to include alongside item within the <li> */
    additionalChild?: React.ReactNode;
    /** Custom item rendering that receives the DropdownContext */
    customChild?: React.ReactNode;
    /** Flag indicating if hitting enter on an item also triggers an arrow down key press */
    enterTriggersArrowDown?: boolean;
    /** An image to display within the InternalDropdownItem, appearing before any component children */
    icon?: React.ReactNode;
    /** Initial focus on the item when the menu is opened (Note: Only applicable to one of the items) */
    autoFocus?: boolean;
    /** A short description of the dropdown item, displayed under the dropdown item content */
    description?: React.ReactNode;
    /** Events to prevent when the item is disabled */
    inoperableEvents?: string[];
}
export declare class InternalDropdownItem extends React.Component<InternalDropdownItemProps> {
    static displayName: string;
    ref: React.RefObject<HTMLLIElement>;
    additionalRef: React.RefObject<any>;
    static defaultProps: InternalDropdownItemProps;
    componentDidMount(): void;
    componentDidUpdate(): void;
    getInnerNode: (node: any) => any;
    onKeyDown: (event: any) => void;
    extendAdditionalChildRef(): React.ReactElement<any, string | React.JSXElementConstructor<any>>;
    componentRef: (element: HTMLLIElement) => void;
    render(): JSX.Element;
}
//# sourceMappingURL=InternalDropdownItem.d.ts.map