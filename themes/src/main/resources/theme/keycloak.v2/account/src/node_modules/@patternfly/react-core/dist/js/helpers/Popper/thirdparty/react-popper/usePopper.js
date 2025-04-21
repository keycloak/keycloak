"use strict";
/* eslint-disable @typescript-eslint/consistent-type-definitions */
Object.defineProperty(exports, "__esModule", { value: true });
exports.usePopper = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const popper_1 = require("../popper-core/popper");
const useIsomorphicLayout_1 = require("../../../../helpers/useIsomorphicLayout");
const isEqual = (a, b) => JSON.stringify(a) === JSON.stringify(b);
/**
 * Simple ponyfill for Object.fromEntries
 */
const fromEntries = (entries) => entries.reduce((acc, [key, value]) => {
    acc[key] = value;
    return acc;
}, {});
const EMPTY_MODIFIERS = [];
const usePopper = (referenceElement, popperElement, options = {}) => {
    const prevOptions = React.useRef(null);
    const optionsWithDefaults = {
        onFirstUpdate: options.onFirstUpdate,
        placement: options.placement || 'bottom',
        strategy: options.strategy || 'absolute',
        modifiers: options.modifiers || EMPTY_MODIFIERS
    };
    const [state, setState] = React.useState({
        styles: {
            popper: {
                position: optionsWithDefaults.strategy,
                left: '0',
                top: '0'
            }
        },
        attributes: {}
    });
    const updateStateModifier = React.useMemo(() => ({
        name: 'updateState',
        enabled: true,
        phase: 'write',
        // eslint-disable-next-line no-shadow
        fn: ({ state }) => {
            const elements = Object.keys(state.elements);
            setState({
                styles: fromEntries(elements.map(element => [element, state.styles[element] || {}])),
                attributes: fromEntries(elements.map(element => [element, state.attributes[element]]))
            });
        },
        requires: ['computeStyles']
    }), []);
    const popperOptions = React.useMemo(() => {
        const newOptions = {
            onFirstUpdate: optionsWithDefaults.onFirstUpdate,
            placement: optionsWithDefaults.placement,
            strategy: optionsWithDefaults.strategy,
            modifiers: [...optionsWithDefaults.modifiers, updateStateModifier, { name: 'applyStyles', enabled: false }]
        };
        if (isEqual(prevOptions.current, newOptions)) {
            return prevOptions.current || newOptions;
        }
        else {
            prevOptions.current = newOptions;
            return newOptions;
        }
    }, [
        optionsWithDefaults.onFirstUpdate,
        optionsWithDefaults.placement,
        optionsWithDefaults.strategy,
        optionsWithDefaults.modifiers,
        updateStateModifier
    ]);
    const popperInstanceRef = React.useRef();
    useIsomorphicLayout_1.useIsomorphicLayoutEffect(() => {
        if (popperInstanceRef && popperInstanceRef.current) {
            popperInstanceRef.current.setOptions(popperOptions);
        }
    }, [popperOptions]);
    useIsomorphicLayout_1.useIsomorphicLayoutEffect(() => {
        if (referenceElement == null || popperElement == null) {
            return;
        }
        const createPopper = options.createPopper || popper_1.createPopper;
        const popperInstance = createPopper(referenceElement, popperElement, popperOptions);
        popperInstanceRef.current = popperInstance;
        return () => {
            popperInstance.destroy();
            popperInstanceRef.current = null;
        };
    }, [referenceElement, popperElement, options.createPopper]);
    return {
        state: popperInstanceRef.current ? popperInstanceRef.current.state : null,
        styles: state.styles,
        attributes: state.attributes,
        update: popperInstanceRef.current ? popperInstanceRef.current.update : null,
        forceUpdate: popperInstanceRef.current ? popperInstanceRef.current.forceUpdate : null
    };
};
exports.usePopper = usePopper;
//# sourceMappingURL=usePopper.js.map