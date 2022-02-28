import * as React from 'react';
export interface InternalDropdownItemProps extends React.HTMLProps<HTMLAnchorElement> {
    /** Anything which can be rendered as dropdown item */
    children?: React.ReactNode;
    /** Classes applied to root element of dropdown item */
    className?: string;
    /** Class applied to list element */
    listItemClassName?: string;
    /** Indicates which component will be used as dropdown item */
    component?: React.ReactNode;
    /** Variant of the item. The 'icon' variant should use DropdownItemIcon to wrap contained icons or images. */
    variant?: 'item' | 'icon';
    /** Role for the item */
    role?: string;
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
}
export declare class InternalDropdownItem extends React.Component<InternalDropdownItemProps> {
    ref: React.RefObject<HTMLLIElement>;
    additionalRef: React.RefObject<any>;
    static defaultProps: InternalDropdownItemProps;
    componentDidMount(): void;
    componentDidUpdate(): void;
    getInnerNode: (node: any) => any;
    onKeyDown: (event: any) => void;
    extendAdditionalChildRef(): React.ReactElement<any, string | ((props: any) => React.ReactElement<any, string | any | (new (props: any) => React.Component<any, any, any>)>) | (new (props: any) => React.Component<any, any, any>)>;
    render(): JSX.Element;
}
