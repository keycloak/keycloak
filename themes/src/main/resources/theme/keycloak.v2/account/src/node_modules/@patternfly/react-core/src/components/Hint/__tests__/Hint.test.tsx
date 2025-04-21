import * as React from 'react';
import { render } from '@testing-library/react';
import { Hint } from '../Hint';
import { HintBody } from '../HintBody';
import { HintTitle } from '../HintTitle';
import { HintFooter } from '../HintFooter';

test('simple hint', () => {
  const { asFragment } = render(
    <Hint>
      <HintTitle>Title</HintTitle>
      <HintBody>Body</HintBody>
      <HintFooter>Footer</HintFooter>
    </Hint>
  );
  expect(asFragment()).toMatchSnapshot();
});
