// @ts-nocheck
import format from './format';
import { modifierPhases } from '../enums';

const INVALID_MODIFIER_ERROR = 'Popper: modifier "%s" provided an invalid %s property, expected %s but got %s';
const MISSING_DEPENDENCY_ERROR = 'Popper: modifier "%s" requires "%s", but "%s" modifier is not available';
const VALID_PROPERTIES = ['name', 'enabled', 'phase', 'fn', 'effect', 'requires', 'options'];

/**
 * @param modifiers
 */
export default function validateModifiers(modifiers: any[]): void {
  modifiers.forEach(modifier => {
    Object.keys(modifier).forEach(key => {
      switch (key) {
        case 'name':
          if (typeof modifier.name !== 'string') {
            console.error(
              format(INVALID_MODIFIER_ERROR, String(modifier.name), '"name"', '"string"', `"${String(modifier.name)}"`)
            );
          }

          break;
        case 'enabled':
          if (typeof modifier.enabled !== 'boolean') {
            console.error(
              format(INVALID_MODIFIER_ERROR, modifier.name, '"enabled"', '"boolean"', `"${String(modifier.enabled)}"`)
            );
          }

        case 'phase':
          if (modifierPhases.indexOf(modifier.phase) < 0) {
            console.error(
              format(
                INVALID_MODIFIER_ERROR,
                modifier.name,
                '"phase"',
                `either ${modifierPhases.join(', ')}`,
                `"${String(modifier.phase)}"`
              )
            );
          }

          break;
        case 'fn':
          if (typeof modifier.fn !== 'function') {
            console.error(
              format(INVALID_MODIFIER_ERROR, modifier.name, '"fn"', '"function"', `"${String(modifier.fn)}"`)
            );
          }

          break;
        case 'effect':
          if (typeof modifier.effect !== 'function') {
            console.error(
              format(INVALID_MODIFIER_ERROR, modifier.name, '"effect"', '"function"', `"${String(modifier.fn)}"`)
            );
          }

          break;
        case 'requires':
          if (!Array.isArray(modifier.requires)) {
            console.error(
              format(INVALID_MODIFIER_ERROR, modifier.name, '"requires"', '"array"', `"${String(modifier.requires)}"`)
            );
          }

          break;
        case 'requiresIfExists':
          if (!Array.isArray(modifier.requiresIfExists)) {
            console.error(
              format(
                INVALID_MODIFIER_ERROR,
                modifier.name,
                '"requiresIfExists"',
                '"array"',
                `"${String(modifier.requiresIfExists)}"`
              )
            );
          }

          break;
        case 'options':
        case 'data':
          break;
        default:
          console.error(
            `PopperJS: an invalid property has been provided to the "${
              modifier.name
            }" modifier, valid properties are ${VALID_PROPERTIES.map(s => `"${s}"`).join(
              ', '
            )}; but "${key}" was provided.`
          );
      }

      modifier.requires &&
        modifier.requires.forEach(requirement => {
          if (modifiers.find(mod => mod.name === requirement) == null) {
            console.error(format(MISSING_DEPENDENCY_ERROR, String(modifier.name), requirement, requirement));
          }
        });
    });
  });
}
