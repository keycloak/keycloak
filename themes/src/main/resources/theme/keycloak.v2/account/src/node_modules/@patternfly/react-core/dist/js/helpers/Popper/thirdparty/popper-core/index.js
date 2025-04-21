"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.detectOverflow = exports.createPopper = exports.popperGenerator = void 0;
const tslib_1 = require("tslib");
const getCompositeRect_1 = tslib_1.__importDefault(require("./dom-utils/getCompositeRect"));
const getLayoutRect_1 = tslib_1.__importDefault(require("./dom-utils/getLayoutRect"));
const listScrollParents_1 = tslib_1.__importDefault(require("./dom-utils/listScrollParents"));
const getOffsetParent_1 = tslib_1.__importDefault(require("./dom-utils/getOffsetParent"));
const getComputedStyle_1 = tslib_1.__importDefault(require("./dom-utils/getComputedStyle"));
const orderModifiers_1 = tslib_1.__importDefault(require("./utils/orderModifiers"));
const debounce_1 = tslib_1.__importDefault(require("./utils/debounce"));
const validateModifiers_1 = tslib_1.__importDefault(require("./utils/validateModifiers"));
const uniqueBy_1 = tslib_1.__importDefault(require("./utils/uniqueBy"));
const getBasePlacement_1 = tslib_1.__importDefault(require("./utils/getBasePlacement"));
const mergeByName_1 = tslib_1.__importDefault(require("./utils/mergeByName"));
const detectOverflow_1 = tslib_1.__importDefault(require("./utils/detectOverflow"));
exports.detectOverflow = detectOverflow_1.default;
const instanceOf_1 = require("./dom-utils/instanceOf");
const enums_1 = require("./enums");
tslib_1.__exportStar(require("./types"), exports);
tslib_1.__exportStar(require("./enums"), exports);
const INVALID_ELEMENT_ERROR = 'Popper: Invalid reference or popper argument provided. They must be either a DOM element or virtual element.';
const INFINITE_LOOP_ERROR = 'Popper: An infinite loop in the modifiers cycle has been detected! The cycle has been interrupted to prevent a browser crash.';
const DEFAULT_OPTIONS = {
    placement: 'bottom',
    modifiers: [],
    strategy: 'absolute'
};
/**
 * @param args
 */
function areValidElements(...args) {
    return !args.some(element => !(element && typeof element.getBoundingClientRect === 'function'));
}
/**
 * @param generatorOptions
 */
function popperGenerator(generatorOptions = {}) {
    const { defaultModifiers = [], defaultOptions = DEFAULT_OPTIONS } = generatorOptions;
    return function createPopper(reference, popper, options = defaultOptions) {
        let state = {
            placement: 'bottom',
            orderedModifiers: [],
            options: Object.assign(Object.assign({}, DEFAULT_OPTIONS), defaultOptions),
            modifiersData: {},
            elements: {
                reference,
                popper
            },
            attributes: {},
            styles: {}
        };
        let effectCleanupFns = [];
        let isDestroyed = false;
        const instance = {
            state,
            setOptions(options) {
                cleanupModifierEffects();
                state.options = Object.assign(Object.assign(Object.assign({}, defaultOptions), state.options), options);
                state.scrollParents = {
                    reference: instanceOf_1.isElement(reference)
                        ? listScrollParents_1.default(reference)
                        : reference.contextElement
                            ? listScrollParents_1.default(reference.contextElement)
                            : [],
                    popper: listScrollParents_1.default(popper)
                };
                // Orders the modifiers based on their dependencies and `phase`
                // properties
                const orderedModifiers = orderModifiers_1.default(mergeByName_1.default([...defaultModifiers, ...state.options.modifiers]));
                // Strip out disabled modifiers
                state.orderedModifiers = orderedModifiers.filter(m => m.enabled);
                // Validate the provided modifiers so that the consumer will get warned
                // if one of the modifiers is invalid for any reason
                if (false /* __DEV__*/) {
                    const modifiers = uniqueBy_1.default([...orderedModifiers, ...state.options.modifiers], ({ name }) => name);
                    validateModifiers_1.default(modifiers);
                    if (getBasePlacement_1.default(state.options.placement) === enums_1.auto) {
                        const flipModifier = state.orderedModifiers.find(({ name }) => name === 'flip');
                        if (!flipModifier) {
                            console.error(['Popper: "auto" placements require the "flip" modifier be', 'present and enabled to work.'].join(' '));
                        }
                    }
                    const { marginTop, marginRight, marginBottom, marginLeft } = getComputedStyle_1.default(popper);
                    // We no longer take into account `margins` on the popper, and it can
                    // cause bugs with positioning, so we'll warn the consumer
                    if ([marginTop, marginRight, marginBottom, marginLeft].some(margin => parseFloat(margin))) {
                        console.warn([
                            'Popper: CSS "margin" styles cannot be used to apply padding',
                            'between the popper and its reference element or boundary.',
                            'To replicate margin, use the `offset` modifier, as well as',
                            'the `padding` option in the `preventOverflow` and `flip`',
                            'modifiers.'
                        ].join(' '));
                    }
                }
                runModifierEffects();
                return instance.update();
            },
            // Sync update – it will always be executed, even if not necessary. This
            // is useful for low frequency updates where sync behavior simplifies the
            // logic.
            // For high frequency updates (e.g. `resize` and `scroll` events), always
            // prefer the async Popper#update method
            forceUpdate() {
                if (isDestroyed) {
                    return;
                }
                const { reference, popper } = state.elements;
                // Don't proceed if `reference` or `popper` are not valid elements
                // anymore
                if (!areValidElements(reference, popper)) {
                    if (false /* __DEV__*/) {
                        console.error(INVALID_ELEMENT_ERROR);
                    }
                    return;
                }
                // Store the reference and popper rects to be read by modifiers
                state.rects = {
                    reference: getCompositeRect_1.default(reference, getOffsetParent_1.default(popper), state.options.strategy === 'fixed'),
                    popper: getLayoutRect_1.default(popper)
                };
                // Modifiers have the ability to reset the current update cycle. The
                // most common use case for this is the `flip` modifier changing the
                // placement, which then needs to re-run all the modifiers, because the
                // logic was previously ran for the previous placement and is therefore
                // stale/incorrect
                state.reset = false;
                state.placement = state.options.placement;
                // On each update cycle, the `modifiersData` property for each modifier
                // is filled with the initial data specified by the modifier. This means
                // it doesn't persist and is fresh on each update.
                // To ensure persistent data, use `${name}#persistent`
                state.orderedModifiers.forEach(modifier => (state.modifiersData[modifier.name] = Object.assign({}, modifier.data)));
                let __debug_loops__ = 0;
                for (let index = 0; index < state.orderedModifiers.length; index++) {
                    if (false /* __DEV__*/) {
                        __debug_loops__ += 1;
                        if (__debug_loops__ > 100) {
                            console.error(INFINITE_LOOP_ERROR);
                            break;
                        }
                    }
                    if (state.reset === true) {
                        state.reset = false;
                        index = -1;
                        continue;
                    }
                    const { fn, options = {}, name } = state.orderedModifiers[index];
                    if (typeof fn === 'function') {
                        state = fn({ state, options, name, instance }) || state;
                    }
                }
            },
            // Async and optimistically optimized update – it will not be executed if
            // not necessary (debounced to run at most once-per-tick)
            update: debounce_1.default(() => new Promise(resolve => {
                instance.forceUpdate();
                resolve(state);
            })),
            destroy() {
                cleanupModifierEffects();
                isDestroyed = true;
            }
        };
        if (!areValidElements(reference, popper)) {
            if (false /* __DEV__*/) {
                console.error(INVALID_ELEMENT_ERROR);
            }
            return instance;
        }
        instance.setOptions(options).then(state => {
            if (!isDestroyed && options.onFirstUpdate) {
                options.onFirstUpdate(state);
            }
        });
        // Modifiers have the ability to execute arbitrary code before the first
        // update cycle runs. They will be executed in the same order as the update
        // cycle. This is useful when a modifier adds some persistent data that
        // other modifiers need to use, but the modifier is run after the dependent
        // one.
        /**
         *
         */
        function runModifierEffects() {
            state.orderedModifiers.forEach(({ name, options = {}, effect }) => {
                if (typeof effect === 'function') {
                    const cleanupFn = effect({ state, name, instance, options });
                    const noopFn = () => { };
                    effectCleanupFns.push(cleanupFn || noopFn);
                }
            });
        }
        /**
         *
         */
        function cleanupModifierEffects() {
            effectCleanupFns.forEach(fn => fn());
            effectCleanupFns = [];
        }
        return instance;
    };
}
exports.popperGenerator = popperGenerator;
exports.createPopper = popperGenerator();
//# sourceMappingURL=index.js.map