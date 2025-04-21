/* eslint-disable @typescript-eslint/consistent-type-definitions */
import * as React from 'react';
import { createPopper as defaultCreatePopper } from '../popper-core/popper';
import { useIsomorphicLayoutEffect } from '../../../../helpers/useIsomorphicLayout';
const isEqual = (a, b) => JSON.stringify(a) === JSON.stringify(b);
/**
 * Simple ponyfill for Object.fromEntries
 */
const fromEntries = (entries) => entries.reduce((acc, [key, value]) => {
    acc[key] = value;
    return acc;
}, {});
const EMPTY_MODIFIERS = [];
export const usePopper = (referenceElement, popperElement, options = {}) => {
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
    useIsomorphicLayoutEffect(() => {
        if (popperInstanceRef && popperInstanceRef.current) {
            popperInstanceRef.current.setOptions(popperOptions);
        }
    }, [popperOptions]);
    useIsomorphicLayoutEffect(() => {
        if (referenceElement == null || popperElement == null) {
            return;
        }
        const createPopper = options.createPopper || defaultCreatePopper;
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
//# sourceMappingURL=usePopper.js.map