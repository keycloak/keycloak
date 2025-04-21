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

interface GenerateIdProps {
  /** String to prefix the random id with */
  prefix?: string;
  /** Component to be rendered with the generated id */
  children(id: string): React.ReactNode;
}

export class GenerateId extends React.Component<GenerateIdProps, {}> {
  static displayName = 'GenerateId';
  static defaultProps = {
    prefix: 'pf-random-id-'
  };
  id = `${this.props.prefix}${currentId++}`;

  render() {
    return this.props.children(this.id);
  }
}
