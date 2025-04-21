/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { PaginationOptionsMenu } from '../../PaginationOptionsMenu';
// any missing imports can usually be resolved by adding them here
import { ToggleTemplateProps } from '../..';

it('PaginationOptionsMenu should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <PaginationOptionsMenu
      className={"''"}
      widgetId={"''"}
      isDisabled={false}
      dropDirection={'up'}
      perPageOptions={[]}
      itemsPerPageTitle={"'Items per page'"}
      page={42}
      perPageSuffix={"'per page'"}
      itemsTitle={"'items'"}
      optionsToggle={"'Select'"}
      itemCount={0}
      firstIndex={0}
      lastIndex={0}
      defaultToFullPage={false}
      perPage={0}
      lastPage={42}
      toggleTemplate={({ firstIndex, lastIndex, itemCount, itemsTitle }: ToggleTemplateProps) => (
        <React.Fragment>
          <b>
            {firstIndex} - {lastIndex}
          </b>{' '}
          of<b>{itemCount}</b> {itemsTitle}
        </React.Fragment>
      )}
      onPerPageSelect={() => null as any}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
