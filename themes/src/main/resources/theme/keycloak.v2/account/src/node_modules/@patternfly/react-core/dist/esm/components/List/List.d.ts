import * as React from 'react';
export declare enum OrderType {
    number = "1",
    lowercaseLetter = "a",
    uppercaseLetter = "A",
    lowercaseRomanNumber = "i",
    uppercaseRomanNumber = "I"
}
export declare enum ListVariant {
    inline = "inline"
}
export declare enum ListComponent {
    ol = "ol",
    ul = "ul"
}
export interface ListProps extends Omit<React.HTMLProps<HTMLUListElement | HTMLOListElement>, 'type'> {
    /** Anything that can be rendered inside of the list */
    children?: React.ReactNode;
    /** Additional classes added to the list */
    className?: string;
    /** Adds list variant styles */
    variant?: ListVariant.inline;
    /** Modifies the list to add borders between items */
    isBordered?: boolean;
    /** Modifies the list to include plain styling */
    isPlain?: boolean;
    /** Modifies the size of the icons in the list */
    iconSize?: 'default' | 'large';
    /** Sets the way items are numbered if variant is set to ordered */
    type?: OrderType;
    component?: 'ol' | 'ul';
}
export declare const List: React.FunctionComponent<ListProps>;
//# sourceMappingURL=List.d.ts.map