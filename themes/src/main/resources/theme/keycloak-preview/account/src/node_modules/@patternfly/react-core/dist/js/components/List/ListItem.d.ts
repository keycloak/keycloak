import * as React from 'react';
export interface ListItemProps extends React.HTMLProps<HTMLLIElement> {
    /** Anything that can be rendered inside of list item */
    children: React.ReactNode;
}
export declare const ListItem: React.FunctionComponent<ListItemProps>;
