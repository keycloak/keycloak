import * as React from 'react';
import { DataListWrapModifier } from './DataList';
export interface DataListItemRowProps extends Omit<React.HTMLProps<HTMLDivElement>, 'children'> {
    /** Content rendered inside the DataListItemRow  */
    children: React.ReactNode;
    /** Additional classes added to the DataListItemRow */
    className?: string;
    /** Id for the row item */
    rowid?: string;
    /** Determines which wrapping modifier to apply to the DataListItemRow */
    wrapModifier?: DataListWrapModifier | 'nowrap' | 'truncate' | 'breakWord';
}
export declare const DataListItemRow: React.FunctionComponent<DataListItemRowProps>;
//# sourceMappingURL=DataListItemRow.d.ts.map