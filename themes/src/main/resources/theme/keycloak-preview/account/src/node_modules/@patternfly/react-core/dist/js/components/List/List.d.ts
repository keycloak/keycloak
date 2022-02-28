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
    /** Sets the way items are numbered if variant is set to ordered */
    type?: OrderType;
    component?: 'ol' | 'ul';
}
export declare const List: React.FunctionComponent<ListProps>;
