import * as React from 'react';
export interface SimpleListItemProps {
    /** id for the item. */
    itemId?: number | string;
    /** Content rendered inside the SimpleList item */
    children?: React.ReactNode;
    /** Additional classes added to the SimpleList <li> */
    className?: string;
    /** Component type of the SimpleList item */
    component?: 'button' | 'a';
    /** Additional classes added to the SimpleList <a> or <button> */
    componentClassName?: string;
    /** Additional props added to the SimpleList <a> or <button> */
    componentProps?: any;
    /** Indicates if the link is current/highlighted */
    isActive?: boolean;
    /** @deprecated please use isActive instead */
    isCurrent?: boolean;
    /** OnClick callback for the SimpleList item */
    onClick?: (event: React.MouseEvent | React.ChangeEvent) => void;
    /** Type of button SimpleList item */
    type?: 'button' | 'submit' | 'reset';
    /** Default hyperlink location */
    href?: string;
}
export declare class SimpleListItem extends React.Component<SimpleListItemProps> {
    static displayName: string;
    ref: React.RefObject<any>;
    static defaultProps: SimpleListItemProps;
    render(): JSX.Element;
}
//# sourceMappingURL=SimpleListItem.d.ts.map