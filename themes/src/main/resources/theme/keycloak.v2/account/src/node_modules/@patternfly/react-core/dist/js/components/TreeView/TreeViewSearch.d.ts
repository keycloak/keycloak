import * as React from 'react';
export interface TreeViewSearchProps extends React.HTMLProps<HTMLInputElement> {
    /** Callback for search input */
    onSearch?: (event: React.ChangeEvent<HTMLInputElement>) => void;
    /** Id for the search input */
    id?: string;
    /** Name for the search input */
    name?: string;
    /** Accessible label for the search input */
    'aria-label'?: string;
    /** Classes applied to the wrapper for the search input */
    className?: string;
}
export declare const TreeViewSearch: React.FunctionComponent<TreeViewSearchProps>;
//# sourceMappingURL=TreeViewSearch.d.ts.map