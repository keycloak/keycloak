declare type OuiaId = number | string;
export interface OUIAProps {
    ouiaId?: OuiaId;
    ouiaSafe?: boolean;
}
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
export declare function getOUIAProps(componentType: string, id: OuiaId, ouiaSafe?: boolean): {
    'data-ouia-component-type': string;
    'data-ouia-safe': boolean;
    'data-ouia-component-id': OuiaId;
};
/**
 * Hooks version of the getOUIAProps function that also memoizes the generated ID
 * Can only be used in functional components
 *
 * @param {string} componentType OUIA component type
 * @param {number|string} id OUIA component id
 * @param {boolean} ouiaSafe false if in animation
 * @param {string} variant Optional variant to add to the generated ID
 */
export declare const useOUIAProps: (componentType: string, id?: OuiaId, ouiaSafe?: boolean, variant?: string) => {
    'data-ouia-component-type': string;
    'data-ouia-safe': boolean;
    'data-ouia-component-id': OuiaId;
};
/**
 * Returns the ID or the memoized generated ID
 *
 * @param {string} componentType OUIA component type
 * @param {number|string} id OUIA component id
 * @param {string} variant Optional variant to add to the generated ID
 */
export declare const useOUIAId: (componentType: string, id?: OuiaId, variant?: string) => OuiaId;
/**
 * Returns a generated id based on the URL location
 *
 * @param {string} componentType OUIA component type
 * @param {string} variant Optional variant to add to the generated ID
 */
export declare function getDefaultOUIAId(componentType: string, variant?: string): string;
export {};
//# sourceMappingURL=ouia.d.ts.map