"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TabsContextConsumer = exports.TabsContextProvider = exports.TabsContext = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
exports.TabsContext = React.createContext({
    variant: 'default',
    mountOnEnter: false,
    unmountOnExit: false,
    localActiveKey: '',
    uniqueId: '',
    handleTabClick: () => null,
    handleTabClose: undefined
});
exports.TabsContextProvider = exports.TabsContext.Provider;
exports.TabsContextConsumer = exports.TabsContext.Consumer;
//# sourceMappingURL=TabsContext.js.map