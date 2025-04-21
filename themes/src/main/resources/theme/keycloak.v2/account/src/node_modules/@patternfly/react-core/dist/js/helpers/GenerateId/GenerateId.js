"use strict";
/** This Component can be used to wrap a functional component in order to generate a random ID
 * Example of how to use this component
 *
 * const Component = ({id}: {id: string}) => (
 *  <GenerateId>{randomId => (
 *     <div id={id || randomId}>
 *       div with random ID
 *     </div>
 *   )}
 *  </GenerateId>
 *  );
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.GenerateId = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
let currentId = 0;
class GenerateId extends React.Component {
    constructor() {
        super(...arguments);
        this.id = `${this.props.prefix}${currentId++}`;
    }
    render() {
        return this.props.children(this.id);
    }
}
exports.GenerateId = GenerateId;
GenerateId.displayName = 'GenerateId';
GenerateId.defaultProps = {
    prefix: 'pf-random-id-'
};
//# sourceMappingURL=GenerateId.js.map