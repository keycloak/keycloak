import React from 'react';
import { render } from '@testing-library/react';
import { Truncate } from '../Truncate';

test('renders default truncation', () => {
  const { asFragment } = render(
    <Truncate content={'Vestibulum interdum risus et enim faucibus, sit amet molestie est accumsan.'} />
  );
  expect(asFragment()).toMatchSnapshot();
});

test('renders start truncation', () => {
  const { asFragment } = render(
    <Truncate
      content={'Vestibulum interdum risus et enim faucibus, sit amet molestie est accumsan.'}
      position={'start'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});

test('renders middle truncation', () => {
  const { asFragment } = render(
    <Truncate
      content={'Vestibulum interdum risus et enim faucibus, sit amet molestie est accumsan.'}
      position={'middle'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
