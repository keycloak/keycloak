import * as React from 'react';
import { render } from '@testing-library/react';
import { AboutModalBox } from '../AboutModalBox';

test('AboutModalBox Test', () => {
  const { asFragment } = render(
    <AboutModalBox aria-labelledby="id" aria-describedby="id2">
      This is a AboutModalBox
    </AboutModalBox>
  );
  expect(asFragment()).toMatchSnapshot();
});
