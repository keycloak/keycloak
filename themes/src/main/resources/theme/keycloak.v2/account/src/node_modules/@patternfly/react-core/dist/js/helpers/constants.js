"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.KeyTypes = exports.ValidatedOptions = exports.KEYHANDLER_DIRECTION = exports.SIDE = exports.KEY_CODES = void 0;
exports.KEY_CODES = { ARROW_UP: 38, ARROW_DOWN: 40, ESCAPE_KEY: 27, TAB: 9, ENTER: 13, SPACE: 32 };
exports.SIDE = { RIGHT: 'right', LEFT: 'left', BOTH: 'both', NONE: 'none' };
exports.KEYHANDLER_DIRECTION = { UP: 'up', DOWN: 'down', RIGHT: 'right', LEFT: 'left' };
var ValidatedOptions;
(function (ValidatedOptions) {
    ValidatedOptions["success"] = "success";
    ValidatedOptions["error"] = "error";
    ValidatedOptions["warning"] = "warning";
    ValidatedOptions["default"] = "default";
})(ValidatedOptions = exports.ValidatedOptions || (exports.ValidatedOptions = {}));
exports.KeyTypes = {
    Tab: 'Tab',
    Space: ' ',
    Escape: 'Escape',
    Enter: 'Enter',
    ArrowUp: 'ArrowUp',
    ArrowDown: 'ArrowDown',
    ArrowLeft: 'ArrowLeft',
    ArrowRight: 'ArrowRight'
};
//# sourceMappingURL=constants.js.map