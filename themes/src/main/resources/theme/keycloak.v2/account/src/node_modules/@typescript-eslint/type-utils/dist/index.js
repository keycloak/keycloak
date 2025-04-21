"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __exportStar = (this && this.__exportStar) || function(m, exports) {
    for (var p in m) if (p !== "default" && !Object.prototype.hasOwnProperty.call(exports, p)) __createBinding(exports, m, p);
};
Object.defineProperty(exports, "__esModule", { value: true });
__exportStar(require("./containsAllTypesByName"), exports);
__exportStar(require("./getConstrainedTypeAtLocation"), exports);
__exportStar(require("./getContextualType"), exports);
__exportStar(require("./getDeclaration"), exports);
__exportStar(require("./getSourceFileOfNode"), exports);
__exportStar(require("./getTokenAtPosition"), exports);
__exportStar(require("./getTypeArguments"), exports);
__exportStar(require("./getTypeName"), exports);
__exportStar(require("./isTypeReadonly"), exports);
__exportStar(require("./isUnsafeAssignment"), exports);
__exportStar(require("./predicates"), exports);
__exportStar(require("./propertyTypes"), exports);
__exportStar(require("./requiresQuoting"), exports);
__exportStar(require("./typeFlagUtils"), exports);
//# sourceMappingURL=index.js.map