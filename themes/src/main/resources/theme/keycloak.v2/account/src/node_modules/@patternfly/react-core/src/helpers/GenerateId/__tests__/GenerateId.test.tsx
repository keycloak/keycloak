import React from 'react';
import { render } from '@testing-library/react';
import { GenerateId } from '../GenerateId';

test('generates id', () => {
  const { asFragment } = render(<GenerateId>{id => <div id={id}>div with random ID</div>}</GenerateId>);

  expect(asFragment()).toMatchSnapshot();
});
