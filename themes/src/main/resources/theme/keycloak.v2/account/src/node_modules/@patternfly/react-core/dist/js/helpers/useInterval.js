"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.useInterval = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
/** This is a custom React hook in a format suggest by Dan Abramov in a blog post here:
 * https://overreacted.io/making-setinterval-declarative-with-react-hooks/. It allows setInterval to be used
 * declaratively in functional React components.
 */
function useInterval(callback, delay) {
    const savedCallback = React.useRef(() => { });
    React.useEffect(() => {
        savedCallback.current = callback;
    }, [callback]);
    React.useEffect(() => {
        function tick() {
            savedCallback.current();
        }
        if (delay !== null) {
            const id = setInterval(tick, delay);
            return () => clearInterval(id);
        }
    }, [delay]);
}
exports.useInterval = useInterval;
//# sourceMappingURL=useInterval.js.map