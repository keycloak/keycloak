import { useMemo } from 'react';

type OuiaId = number | string;

// https://ouia.readthedocs.io/en/latest/README.html#ouia-component
export interface OUIAProps {
  // If there is only one instance of the component on the page at once, it is OPTIONAL
  ouiaId?: OuiaId;
  // False if in animation
  ouiaSafe?: boolean;
}

let uid = 0;
const ouiaPrefix = 'OUIA-Generated-';
const ouiaIdByRoute: any = {};

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
export function getOUIAProps(componentType: string, id: OuiaId, ouiaSafe: boolean = true) {
  return {
    'data-ouia-component-type': `PF4/${componentType}`,
    'data-ouia-safe': ouiaSafe,
    'data-ouia-component-id': id
  };
}

/**
 * Hooks version of the getOUIAProps function that also memoizes the generated ID
 * Can only be used in functional components
 *
 * @param {string} componentType OUIA component type
 * @param {number|string} id OUIA component id
 * @param {boolean} ouiaSafe false if in animation
 * @param {string} variant Optional variant to add to the generated ID
 */
export const useOUIAProps = (componentType: string, id?: OuiaId, ouiaSafe: boolean = true, variant?: string) => ({
  'data-ouia-component-type': `PF4/${componentType}`,
  'data-ouia-safe': ouiaSafe,
  'data-ouia-component-id': useOUIAId(componentType, id, variant)
});

/**
 * Returns the ID or the memoized generated ID
 *
 * @param {string} componentType OUIA component type
 * @param {number|string} id OUIA component id
 * @param {string} variant Optional variant to add to the generated ID
 */
export const useOUIAId = (componentType: string, id?: OuiaId, variant?: string) => {
  if (id !== undefined) {
    return id;
  }
  return useMemo(() => getDefaultOUIAId(componentType, variant), [componentType, variant]);
};

/**
 * Returns a generated id based on the URL location
 *
 * @param {string} componentType OUIA component type
 * @param {string} variant Optional variant to add to the generated ID
 */
export function getDefaultOUIAId(componentType: string, variant?: string) {
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
    } else {
      // node/SSR environments
      key = `${componentType}-${variant || ''}`;
    }
    if (!ouiaIdByRoute[key]) {
      ouiaIdByRoute[key] = 0;
    }
    return `${ouiaPrefix}${componentType}-${variant ? `${variant}-` : ''}${++ouiaIdByRoute[key]}`;
  } catch (exception) {
    return `${ouiaPrefix}${componentType}-${variant ? `${variant}-` : ''}${++uid}`;
  }
}
