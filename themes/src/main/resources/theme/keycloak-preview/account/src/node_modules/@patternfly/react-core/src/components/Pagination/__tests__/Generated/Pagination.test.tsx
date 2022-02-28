/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Pagination } from '../../Pagination';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Pagination should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Pagination
      children={<>ReactNode</>}
      className={"''"}
      itemCount={42}
      variant={'top'}
      isDisabled={false}
      isCompact={false}
      perPage={42}
      perPageOptions={[
        {
          title: '10',
          value: 10
        },
        {
          title: '20',
          value: 20
        },
        {
          title: '50',
          value: 50
        },
        {
          title: '100',
          value: 100
        }
      ]}
      defaultToFullPage={false}
      firstPage={1}
      page={0}
      offset={0}
      itemsStart={null}
      itemsEnd={null}
      widgetId={"'pagination-options-menu'"}
      dropDirection={'up'}
      titles={undefined}
      toggleTemplate={'string'}
      onSetPage={() => undefined}
      onFirstClick={() => undefined}
      onPreviousClick={() => undefined}
      onNextClick={() => undefined}
      onLastClick={() => undefined}
      onPageInput={() => undefined}
      onPerPageSelect={() => undefined}
    />
  );
  expect(view).toMatchSnapshot();
});
