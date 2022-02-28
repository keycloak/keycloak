/**
 * @flow
 */

import JSXAttributeMock from './JSXAttributeMock';

export default function JSXElementMock(
  tagName: string,
  attributes: Array<JSXAttributeMock> = [],
  children: Array<Node> = [],
) {
  return {
    type: 'JSXElement',
    openingElement: {
      type: 'JSXOpeningElement',
      name: {
        type: 'JSXIdentifier',
        name: tagName,
      },
      attributes,
    },
    children,
  };
}
