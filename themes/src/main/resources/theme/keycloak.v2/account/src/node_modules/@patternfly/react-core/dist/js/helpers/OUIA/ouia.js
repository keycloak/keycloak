"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getDefaultOUIAId = exports.useOUIAId = exports.useOUIAProps = exports.getOUIAProps = void 0;
const react_1 = require("react");
let uid = 0;
const ouiaPrefix = 'OUIA-Generated-';
const ouiaIdByRoute = {};
/** Get props to conform to OUIA spec
 *
 * For functional components, use the useOUIAProps function instead
 *
 * In class based components, create a state variable ouiaStateId to create a static generated ID:
 * state = {
 *  ouiaStateId: getDefaultOUIAId(Chip.displayName)
 * }
 * This generated ID should remain alive as long as the component is not unmounted.
 *
 * Then add the attributes to the component
 * {...getOUIAProps('OverflowChip', this.props.ouiaId !== undefined ? this.props.ouiaId : this.state.ouiaStateId)}
 *
 * @param {string} componentType OUIA component type
 * @param {number|string} id OUIA component id
 * @param {boolean} ouiaSafe false if in animation
 */
function getOUIAProps(componentType, id, ouiaSafe = true) {
    return {
        'data-ouia-component-type': `PF4/${componentType}`,
        'data-ouia-safe': ouiaSafe,
        'data-ouia-component-id': id
    };
}
exports.getOUIAProps = getOUIAProps;
/**
 * Hooks version of the getOUIAProps function that also memoizes the generated ID
 * Can only be used in functional components
 *
 * @param {string} componentType OUIA component type
 * @param {number|string} id OUIA component id
 * @param {boolean} ouiaSafe false if in animation
 * @param {string} variant Optional variant to add to the generated ID
 */
const useOUIAProps = (componentType, id, ouiaSafe = true, variant) => ({
    'data-ouia-component-type': `PF4/${componentType}`,
    'data-ouia-safe': ouiaSafe,
    'data-ouia-component-id': exports.useOUIAId(componentType, id, variant)
});
exports.useOUIAProps = useOUIAProps;
/**
 * Returns the ID or the memoized generated ID
 *
 * @param {string} componentType OUIA component type
 * @param {number|string} id OUIA component id
 * @param {string} variant Optional variant to add to the generated ID
 */
const useOUIAId = (componentType, id, variant) => {
    if (id !== undefined) {
        return id;
    }
    return react_1.useMemo(() => getDefaultOUIAId(componentType, variant), [componentType, variant]);
};
exports.useOUIAId = useOUIAId;
/**
 * Returns a generated id based on the URL location
 *
 * @param {string} componentType OUIA component type
 * @param {string} variant Optional variant to add to the generated ID
 */
function getDefaultOUIAId(componentType, variant) {
    /*
    ouiaIdByRoute = {
      [route+componentType]: [number]
    }
    */
    try {
        let key;
        if (typeof window !== 'undefined') {
            // browser environments
            key = `${window.location.href}-${componentType}-${variant || ''}`;
        }
        else {
            // node/SSR environments
            key = `${componentType}-${variant || ''}`;
        }
        if (!ouiaIdByRoute[key]) {
            ouiaIdByRoute[key] = 0;
        }
        return `${ouiaPrefix}${componentType}-${variant ? `${variant}-` : ''}${++ouiaIdByRoute[key]}`;
    }
    catch (exception) {
        return `${ouiaPrefix}${componentType}-${variant ? `${variant}-` : ''}${++uid}`;
    }
}
exports.getDefaultOUIAId = getDefaultOUIAId;
//# sourceMappingURL=ouia.js.map