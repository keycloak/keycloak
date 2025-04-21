import React from 'react';
import { CardContext } from '../Card';
import { CardExpandableContent } from '../CardExpandableContent';
import { render } from '@testing-library/react';

test('renders successfully', () => {
  const { asFragment } = render(
    <CardContext.Provider value={{ isExpanded: true }}>
      <CardExpandableContent />
    </CardContext.Provider>
  );
  expect(asFragment()).toMatchSnapshot();
});
