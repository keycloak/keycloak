import _pt from "prop-types";

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

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

class GenerateId extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "id", `${this.props.prefix}${currentId++}`);
  }

  render() {
    return this.props.children(this.id);
  }

}

_defineProperty(GenerateId, "propTypes", {
  prefix: _pt.string
});

_defineProperty(GenerateId, "defaultProps", {
  prefix: 'pf-random-id-'
});

export default GenerateId;
//# sourceMappingURL=GenerateId.js.map