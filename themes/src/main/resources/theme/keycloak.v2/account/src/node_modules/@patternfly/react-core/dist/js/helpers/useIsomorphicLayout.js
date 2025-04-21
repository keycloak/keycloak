"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.useIsomorphicLayoutEffect = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const util_1 = require("./util");
/**
 * Small wrapper around `useLayoutEffect` to get rid of the warning on SSR envs
 */
exports.useIsomorphicLayoutEffect = util_1.canUseDOM ? React.useLayoutEffect : React.useEffect;
//# sourceMappingURL=useIsomorphicLayout.js.map