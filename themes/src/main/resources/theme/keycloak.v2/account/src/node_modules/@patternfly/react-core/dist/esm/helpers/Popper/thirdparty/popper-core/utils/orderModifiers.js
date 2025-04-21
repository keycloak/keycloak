import { modifierPhases } from '../enums';
// source: https://stackoverflow.com/questions/49875255
/**
 * @param modifiers
 */
function order(modifiers) {
    const map = new Map();
    const visited = new Set();
    const result = [];
    modifiers.forEach(modifier => {
        map.set(modifier.name, modifier);
    });
    // On visiting object, check for its dependencies and visit them recursively
    /**
     * @param modifier
     */
    function sort(modifier) {
        visited.add(modifier.name);
        const requires = [...(modifier.requires || []), ...(modifier.requiresIfExists || [])];
        requires.forEach(dep => {
            if (!visited.has(dep)) {
                const depModifier = map.get(dep);
                if (depModifier) {
                    sort(depModifier);
                }
            }
        });
        result.push(modifier);
    }
    modifiers.forEach(modifier => {
        if (!visited.has(modifier.name)) {
            // check for visited object
            sort(modifier);
        }
    });
    return result;
}
/**
 * @param modifiers
 */
export default function orderModifiers(modifiers) {
    // order based on dependencies
    const orderedModifiers = order(modifiers);
    // order based on phase
    return modifierPhases.reduce((acc, phase) => acc.concat(orderedModifiers.filter(modifier => modifier.phase === phase)), []);
}
//# sourceMappingURL=orderModifiers.js.map