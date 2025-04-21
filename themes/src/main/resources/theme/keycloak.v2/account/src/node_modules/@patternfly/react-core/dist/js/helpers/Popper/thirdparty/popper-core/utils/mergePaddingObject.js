"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const getFreshSideObject_1 = tslib_1.__importDefault(require("./getFreshSideObject"));
/**
 * @param paddingObject
 */
function mergePaddingObject(paddingObject) {
    return Object.assign(Object.assign({}, getFreshSideObject_1.default()), paddingObject);
}
exports.default = mergePaddingObject;
//# sourceMappingURL=mergePaddingObject.js.map