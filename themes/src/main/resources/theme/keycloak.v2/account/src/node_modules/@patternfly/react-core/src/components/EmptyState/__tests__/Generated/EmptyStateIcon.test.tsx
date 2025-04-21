/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { UserIcon } from '@patternfly/react-icons';
import { EmptyStateIcon } from '../../EmptyStateIcon';
// any missing imports can usually be resolved by adding them here
import {} from '../..';
it('EmptyStateIcon should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <EmptyStateIcon
      color={'string'}
      title={'string'}
      className={"''"}
      icon={UserIcon}
      component={null}
      variant={'icon'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});

it('EmptyStateIcon should match snapshot for variant container', () => {
  const { asFragment } = render(
    <EmptyStateIcon
      color={'string'}
      title={'string'}
      className={"''"}
      icon={null}
      component={() => <div>Component</div>}
      variant={'container'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
