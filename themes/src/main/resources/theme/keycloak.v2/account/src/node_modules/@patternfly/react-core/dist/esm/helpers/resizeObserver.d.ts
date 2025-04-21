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
export declare const getResizeObserver: (containerRefElement: Element, handleResize: () => void) => () => void;
//# sourceMappingURL=resizeObserver.d.ts.map