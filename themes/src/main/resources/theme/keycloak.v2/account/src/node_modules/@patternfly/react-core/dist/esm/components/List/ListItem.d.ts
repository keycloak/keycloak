import * as React from 'react';
export interface ListItemProps extends React.HTMLProps<HTMLLIElement> {
    /** Icon for the list item */
    icon?: React.ReactNode | null;
    /** Anything that can be rendered inside of list item */
    children: React.ReactNode;
}
export declare const ListItem: React.FunctionComponent<ListItemProps>;
//# sourceMappingURL=ListItem.d.ts.map