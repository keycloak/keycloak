"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getResizeObserver = void 0;
const util_1 = require("./util");
/**
 * This function creates a ResizeObserver used to handle resize events for the given containerRef. If ResizeObserver
 * or the given containerRef are not available, a window resize event listener is used by default.
 *
 * Example 1:
 *
 * private containerRef = React.createRef<HTMLDivElement>();
 * private observer: any = () => {};
 *
 * public componentDidMount() {
 *   this.observer = getResizeObserver(this.containerRef.current, this.handleResize);
 * }
 *
 * public componentWillUnmount() {
 *   this.observer();
 * }
 *
 * private handleResize = () => {
 *   if (this.containerRef.current && this.containerRef.current.clientWidth) {
 *     this.setState({ width: this.containerRef.current.clientWidth });
 *   }
 * };
 *
 * public render() {
 *   return (
 *     <div ref={this.containerRef} >
 *       <Chart width={this.state.width} ... />
 *     </div>
 *   );
 * }
 *
 * Example 2:
 *
 * private inputRef = React.createRef<HTMLInputElement>();
 * private observer: any = () => {};
 *
 * public componentDidMount() {
 *   this.observer = getResizeObserver(this.inputRef.current, this.handleResize);
 * }
 *
 * public componentWillUnmount() {
 *   this.observer();
 * }
 *
 * private handleResize = () => {
 *   if (this.inputRef.current) {
 *     trimLeft(inputRef.current, String(this.props.value));
 *   }
 * };
 *
 * public render() {
 *   return (
 *     <input ref={this.inputRef} ... />
 *   );
 * }
 *
 * @param {Element} containerRefElement The container reference to observe
 * @param {Function} handleResize The function to call for resize events
 * @return {Function} The function used to unobserve resize events
 */
const getResizeObserver = (containerRefElement, handleResize) => {
    let unobserve;
    if (util_1.canUseDOM) {
        const { ResizeObserver } = window;
        if (containerRefElement && ResizeObserver) {
            const resizeObserver = new ResizeObserver((entries) => {
                // Wrap resize function in requestAnimationFrame to avoid "ResizeObserver loop limit exceeded" errors
                window.requestAnimationFrame(() => {
                    if (Array.isArray(entries) && entries.length > 0) {
                        handleResize();
                    }
                });
            });
            resizeObserver.observe(containerRefElement);
            unobserve = () => resizeObserver.unobserve(containerRefElement);
        }
        else {
            window.addEventListener('resize', handleResize);
            unobserve = () => window.removeEventListener('resize', handleResize);
        }
    }
    return () => {
        if (unobserve) {
            unobserve();
        }
    };
};
exports.getResizeObserver = getResizeObserver;
//# sourceMappingURL=resizeObserver.js.map