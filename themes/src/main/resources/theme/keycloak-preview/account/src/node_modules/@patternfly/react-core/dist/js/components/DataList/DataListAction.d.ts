import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
interface IDataListActionVisibility {
    hidden?: string;
    hiddenOnSm?: string;
    hiddenOnMd?: string;
    hiddenOnLg?: string;
    hiddenOnXl?: string;
    hiddenOn2Xl?: string;
    visibleOnSm?: string;
    visibleOnMd?: string;
    visibleOnLg?: string;
    visibleOnXl?: string;
    visibleOn2Xl?: string;
}
export declare const DataListActionVisibility: IDataListActionVisibility;
export interface DataListActionProps extends Omit<React.HTMLProps<HTMLDivElement>, 'children'> {
    /** Content rendered as DataList Action  (e.g <Button> or <Dropdown>) */
    children: React.ReactNode;
    /** Additional classes added to the DataList Action */
    className?: string;
    /** Identify the DataList toggle number */
    id: string;
    /** Adds accessible text to the DataList Action */
    'aria-labelledby': string;
    /** Adds accessible text to the DataList Action */
    'aria-label': string;
}
interface DataListActionState {
    isOpen: boolean;
}
export declare class DataListAction extends React.Component<DataListActionProps, DataListActionState> {
    static defaultProps: PickOptional<DataListActionProps>;
    constructor(props: DataListActionProps);
    onToggle: (isOpen: boolean) => void;
    onSelect: (event: MouseEvent) => void;
    render(): JSX.Element;
}
export {};
