import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
import { DrawerColorVariant, DrawerContext } from './Drawer';
import { formatBreakpointMods } from '../../helpers/util';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
let isResizing = null;
let newSize = 0;
export const DrawerPanelContent = (_a) => {
    var { className = '', id, children, hasNoBorder = false, isResizable = false, onResize, minSize, defaultSize, maxSize, increment = 5, resizeAriaLabel = 'Resize', widths, colorVariant = DrawerColorVariant.default } = _a, props = __rest(_a, ["className", "id", "children", "hasNoBorder", "isResizable", "onResize", "minSize", "defaultSize", "maxSize", "increment", "resizeAriaLabel", "widths", "colorVariant"]);
    const panel = React.useRef();
    const splitterRef = React.useRef();
    const [separatorValue, setSeparatorValue] = React.useState(0);
    const { position, isExpanded, isStatic, onExpand, drawerRef, drawerContentRef, isInline } = React.useContext(DrawerContext);
    const hidden = isStatic ? false : !isExpanded;
    const [isExpandedInternal, setIsExpandedInternal] = React.useState(!hidden);
    let currWidth = 0;
    let panelRect;
    let right;
    let left;
    let bottom;
    let setInitialVals = true;
    React.useEffect(() => {
        if (!isStatic && isExpanded) {
            setIsExpandedInternal(isExpanded);
        }
    }, [isStatic, isExpanded]);
    const calcValueNow = () => {
        let splitterPos;
        let drawerSize;
        if (isInline && position === 'right') {
            splitterPos = panel.current.getBoundingClientRect().right - splitterRef.current.getBoundingClientRect().left;
            drawerSize = drawerRef.current.getBoundingClientRect().right - drawerRef.current.getBoundingClientRect().left;
        }
        else if (isInline && position === 'left') {
            splitterPos = splitterRef.current.getBoundingClientRect().right - panel.current.getBoundingClientRect().left;
            drawerSize = drawerRef.current.getBoundingClientRect().right - drawerRef.current.getBoundingClientRect().left;
        }
        else if (position === 'right') {
            splitterPos =
                drawerContentRef.current.getBoundingClientRect().right - splitterRef.current.getBoundingClientRect().left;
            drawerSize =
                drawerContentRef.current.getBoundingClientRect().right - drawerContentRef.current.getBoundingClientRect().left;
        }
        else if (position === 'left') {
            splitterPos =
                splitterRef.current.getBoundingClientRect().right - drawerContentRef.current.getBoundingClientRect().left;
            drawerSize =
                drawerContentRef.current.getBoundingClientRect().right - drawerContentRef.current.getBoundingClientRect().left;
        }
        else if (position === 'bottom') {
            splitterPos =
                drawerContentRef.current.getBoundingClientRect().bottom - splitterRef.current.getBoundingClientRect().top;
            drawerSize =
                drawerContentRef.current.getBoundingClientRect().bottom - drawerContentRef.current.getBoundingClientRect().top;
        }
        const newSplitterPos = (splitterPos / drawerSize) * 100;
        return Math.round((newSplitterPos + Number.EPSILON) * 100) / 100;
    };
    const handleTouchStart = (e) => {
        e.stopPropagation();
        document.addEventListener('touchmove', callbackTouchMove, { passive: false });
        document.addEventListener('touchend', callbackTouchEnd);
        isResizing = true;
    };
    const handleMousedown = (e) => {
        e.stopPropagation();
        e.preventDefault();
        document.addEventListener('mousemove', callbackMouseMove);
        document.addEventListener('mouseup', callbackMouseUp);
        drawerRef.current.classList.add(css(styles.modifiers.resizing));
        isResizing = true;
        setInitialVals = true;
    };
    const handleMouseMove = (e) => {
        const mousePos = position === 'bottom' ? e.clientY : e.clientX;
        handleControlMove(e, mousePos);
    };
    const handleTouchMove = (e) => {
        e.preventDefault();
        e.stopImmediatePropagation();
        const touchPos = position === 'bottom' ? e.touches[0].clientY : e.touches[0].clientX;
        handleControlMove(e, touchPos);
    };
    const handleControlMove = (e, controlPosition) => {
        e.stopPropagation();
        if (!isResizing) {
            return;
        }
        if (setInitialVals) {
            panelRect = panel.current.getBoundingClientRect();
            right = panelRect.right;
            left = panelRect.left;
            bottom = panelRect.bottom;
            setInitialVals = false;
        }
        const mousePos = controlPosition;
        let newSize = 0;
        if (position === 'right') {
            newSize = right - mousePos;
        }
        else if (position === 'left') {
            newSize = mousePos - left;
        }
        else {
            newSize = bottom - mousePos;
        }
        if (position === 'bottom') {
            panel.current.style.overflowAnchor = 'none';
        }
        panel.current.style.setProperty('--pf-c-drawer__panel--md--FlexBasis', newSize + 'px');
        currWidth = newSize;
        setSeparatorValue(calcValueNow());
    };
    const handleMouseup = () => {
        if (!isResizing) {
            return;
        }
        drawerRef.current.classList.remove(css(styles.modifiers.resizing));
        isResizing = false;
        onResize && onResize(currWidth, id);
        setInitialVals = true;
        document.removeEventListener('mousemove', callbackMouseMove);
        document.removeEventListener('mouseup', callbackMouseUp);
    };
    const handleTouchEnd = (e) => {
        e.stopPropagation();
        if (!isResizing) {
            return;
        }
        isResizing = false;
        onResize && onResize(currWidth, id);
        document.removeEventListener('touchmove', callbackTouchMove);
        document.removeEventListener('touchend', callbackTouchEnd);
    };
    const callbackMouseMove = React.useCallback(handleMouseMove, []);
    const callbackTouchEnd = React.useCallback(handleTouchEnd, []);
    const callbackTouchMove = React.useCallback(handleTouchMove, []);
    const callbackMouseUp = React.useCallback(handleMouseup, []);
    const handleKeys = (e) => {
        const key = e.key;
        if (key !== 'Escape' &&
            key !== 'Enter' &&
            key !== 'ArrowUp' &&
            key !== 'ArrowDown' &&
            key !== 'ArrowLeft' &&
            key !== 'ArrowRight') {
            if (isResizing) {
                e.preventDefault();
            }
            return;
        }
        e.preventDefault();
        if (key === 'Escape' || key === 'Enter') {
            onResize && onResize(currWidth, id);
        }
        const panelRect = panel.current.getBoundingClientRect();
        newSize = position === 'bottom' ? panelRect.height : panelRect.width;
        let delta = 0;
        if (key === 'ArrowRight') {
            delta = position === 'left' ? increment : -increment;
        }
        else if (key === 'ArrowLeft') {
            delta = position === 'left' ? -increment : increment;
        }
        else if (key === 'ArrowUp') {
            delta = increment;
        }
        else if (key === 'ArrowDown') {
            delta = -increment;
        }
        newSize = newSize + delta;
        if (position === 'bottom') {
            panel.current.style.overflowAnchor = 'none';
        }
        panel.current.style.setProperty('--pf-c-drawer__panel--md--FlexBasis', newSize + 'px');
        currWidth = newSize;
        setSeparatorValue(calcValueNow());
    };
    const boundaryCssVars = {};
    if (defaultSize) {
        boundaryCssVars['--pf-c-drawer__panel--md--FlexBasis'] = defaultSize;
    }
    if (minSize) {
        boundaryCssVars['--pf-c-drawer__panel--md--FlexBasis--min'] = minSize;
    }
    if (maxSize) {
        boundaryCssVars['--pf-c-drawer__panel--md--FlexBasis--max'] = maxSize;
    }
    return (React.createElement(GenerateId, { prefix: "pf-drawer-panel-" }, panelId => (React.createElement("div", Object.assign({ id: id || panelId, className: css(styles.drawerPanel, isResizable && styles.modifiers.resizable, hasNoBorder && styles.modifiers.noBorder, formatBreakpointMods(widths, styles), colorVariant === DrawerColorVariant.light200 && styles.modifiers.light_200, className), ref: panel, onTransitionEnd: ev => {
            if (!hidden && ev.nativeEvent.propertyName === 'transform') {
                onExpand();
            }
            setIsExpandedInternal(!hidden);
        }, hidden: hidden }, ((defaultSize || minSize || maxSize) && {
        style: boundaryCssVars
    }), props), isExpandedInternal && (React.createElement(React.Fragment, null,
        isResizable && (React.createElement(React.Fragment, null,
            React.createElement("div", { className: css(styles.drawerSplitter, position !== 'bottom' && styles.modifiers.vertical), role: "separator", tabIndex: 0, "aria-orientation": position === 'bottom' ? 'horizontal' : 'vertical', "aria-label": resizeAriaLabel, "aria-valuenow": separatorValue, "aria-valuemin": 0, "aria-valuemax": 100, "aria-controls": id || panelId, onMouseDown: handleMousedown, onKeyDown: handleKeys, onTouchStart: handleTouchStart, ref: splitterRef },
                React.createElement("div", { className: css(styles.drawerSplitterHandle), "aria-hidden": true })),
            React.createElement("div", { className: css(styles.drawerPanelMain) }, children))),
        !isResizable && children))))));
};
DrawerPanelContent.displayName = 'DrawerPanelContent';
//# sourceMappingURL=DrawerPanelContent.js.map