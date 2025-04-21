import * as React from 'react';
export interface TabsContextProps {
    variant: 'default' | 'light300';
    mountOnEnter: boolean;
    unmountOnExit: boolean;
    localActiveKey: string | number;
    uniqueId: string;
    handleTabClick: (event: React.MouseEvent<HTMLElement, MouseEvent>, eventKey: number | string, tabContentRef: React.RefObject<any>) => void;
    handleTabClose?: (event: React.MouseEvent<HTMLElement, MouseEvent>, eventKey: number | string, tabContentRef?: React.RefObject<any>) => void;
}
export declare const TabsContext: React.Context<TabsContextProps>;
export declare const TabsContextProvider: React.Provider<TabsContextProps>;
export declare const TabsContextConsumer: React.Consumer<TabsContextProps>;
//# sourceMappingURL=TabsContext.d.ts.map