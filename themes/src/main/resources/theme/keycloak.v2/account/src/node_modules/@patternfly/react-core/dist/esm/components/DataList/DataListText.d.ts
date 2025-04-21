import * as React from 'react';
import { DataListWrapModifier } from './DataList';
export interface DataListTextProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered within the data list text */
    children?: React.ReactNode;
    /** Additional classes added to the data list text */
    className?: string;
    /** Determines which element to render as a data list text. Usually div or span */
    component?: React.ReactNode;
    /** Determines which wrapping modifier to apply to the data list text */
    wrapModifier?: DataListWrapModifier | 'nowrap' | 'truncate' | 'breakWord';
    /** text to display on the tooltip */
    tooltip?: string;
    /** callback used to create the tooltip if text is truncated */
    onMouseEnter?: (event: any) => void;
}
export declare const DataListText: React.FunctionComponent<DataListTextProps>;
//# sourceMappingURL=DataListText.d.ts.map