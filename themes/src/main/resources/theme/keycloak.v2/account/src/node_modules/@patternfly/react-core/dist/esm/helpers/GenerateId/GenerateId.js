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
import * as React from 'react';
let currentId = 0;
export class GenerateId extends React.Component {
    constructor() {
        super(...arguments);
        this.id = `${this.props.prefix}${currentId++}`;
    }
    render() {
        return this.props.children(this.id);
    }
}
GenerateId.displayName = 'GenerateId';
GenerateId.defaultProps = {
    prefix: 'pf-random-id-'
};
//# sourceMappingURL=GenerateId.js.map