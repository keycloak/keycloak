"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
// @ts-nocheck
const format_1 = tslib_1.__importDefault(require("./format"));
const enums_1 = require("../enums");
const INVALID_MODIFIER_ERROR = 'Popper: modifier "%s" provided an invalid %s property, expected %s but got %s';
const MISSING_DEPENDENCY_ERROR = 'Popper: modifier "%s" requires "%s", but "%s" modifier is not available';
const VALID_PROPERTIES = ['name', 'enabled', 'phase', 'fn', 'effect', 'requires', 'options'];
/**
 * @param modifiers
 */
function validateModifiers(modifiers) {
    modifiers.forEach(modifier => {
        Object.keys(modifier).forEach(key => {
            switch (key) {
                case 'name':
                    if (typeof modifier.name !== 'string') {
                        console.error(format_1.default(INVALID_MODIFIER_ERROR, String(modifier.name), '"name"', '"string"', `"${String(modifier.name)}"`));
                    }
                    break;
                case 'enabled':
                    if (typeof modifier.enabled !== 'boolean') {
                        console.error(format_1.default(INVALID_MODIFIER_ERROR, modifier.name, '"enabled"', '"boolean"', `"${String(modifier.enabled)}"`));
                    }
                case 'phase':
                    if (enums_1.modifierPhases.indexOf(modifier.phase) < 0) {
                        console.error(format_1.default(INVALID_MODIFIER_ERROR, modifier.name, '"phase"', `either ${enums_1.modifierPhases.join(', ')}`, `"${String(modifier.phase)}"`));
                    }
                    break;
                case 'fn':
                    if (typeof modifier.fn !== 'function') {
                        console.error(format_1.default(INVALID_MODIFIER_ERROR, modifier.name, '"fn"', '"function"', `"${String(modifier.fn)}"`));
                    }
                    break;
                case 'effect':
                    if (typeof modifier.effect !== 'function') {
                        console.error(format_1.default(INVALID_MODIFIER_ERROR, modifier.name, '"effect"', '"function"', `"${String(modifier.fn)}"`));
                    }
                    break;
                case 'requires':
                    if (!Array.isArray(modifier.requires)) {
                        console.error(format_1.default(INVALID_MODIFIER_ERROR, modifier.name, '"requires"', '"array"', `"${String(modifier.requires)}"`));
                    }
                    break;
                case 'requiresIfExists':
                    if (!Array.isArray(modifier.requiresIfExists)) {
                        console.error(format_1.default(INVALID_MODIFIER_ERROR, modifier.name, '"requiresIfExists"', '"array"', `"${String(modifier.requiresIfExists)}"`));
                    }
                    break;
                case 'options':
                case 'data':
                    break;
                default:
                    console.error(`PopperJS: an invalid property has been provided to the "${modifier.name}" modifier, valid properties are ${VALID_PROPERTIES.map(s => `"${s}"`).join(', ')}; but "${key}" was provided.`);
            }
            modifier.requires &&
                modifier.requires.forEach(requirement => {
                    if (modifiers.find(mod => mod.name === requirement) == null) {
                        console.error(format_1.default(MISSING_DEPENDENCY_ERROR, String(modifier.name), requirement, requirement));
                    }
                });
        });
    });
}
exports.default = validateModifiers;
//# sourceMappingURL=validateModifiers.js.map