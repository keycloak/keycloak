import * as React from 'react';
import { DropdownProps } from './Dropdown';
import { OUIAProps } from '../../helpers';
import { PickOptional } from '../../helpers/typeUtils';
export declare class DropdownWithContext extends React.Component<DropdownProps & OUIAProps> {
    static displayName: string;
    openedOnEnter: boolean;
    baseComponentRef: React.RefObject<any>;
    menuComponentRef: React.RefObject<any>;
    static currentId: number;
    static defaultProps: PickOptional<DropdownProps>;
    constructor(props: DropdownProps & OUIAProps);
    onEnter: () => void;
    componentDidUpdate(): void;
    setMenuComponentRef: (element: any) => void;
    getMenuComponentRef: () => React.RefObject<any>;
    render(): JSX.Element;
}
//# sourceMappingURL=DropdownWithContext.d.ts.map