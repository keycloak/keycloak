import * as React from 'react';
export const TabsContext = React.createContext({
    variant: 'default',
    mountOnEnter: false,
    unmountOnExit: false,
    localActiveKey: '',
    uniqueId: '',
    handleTabClick: () => null,
    handleTabClose: undefined
});
export const TabsContextProvider = TabsContext.Provider;
export const TabsContextConsumer = TabsContext.Consumer;
//# sourceMappingURL=TabsContext.js.map