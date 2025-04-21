import * as React from 'react';
import { Grid } from '../Grid';
import { GridItem } from '../GridItem';
import { render } from '@testing-library/react';

test('gutter', () => {
  const { asFragment } = render(<Grid hasGutter />);
  expect(asFragment()).toMatchSnapshot();
});

test('alternative component', () => {
  const { asFragment } = render(
    <Grid component="ul">
      <GridItem component="li">Test</GridItem>
    </Grid>
  );
  expect(asFragment()).toMatchSnapshot();
});
