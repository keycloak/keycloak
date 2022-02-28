import * as React from 'react';
import { shallow } from 'enzyme';
import { Tooltip } from '../Tooltip';

test('tooltip renders', () => {
  const view = shallow(
    <Tooltip
      position="top"
      content={
        <div>
          Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.
        </div>
      }
    >
      <div>Toggle tooltip</div>
    </Tooltip>
  );
  expect(view).toMatchSnapshot();
});

test('tooltip passes along values to tippy.js', () => {
  const view = shallow(
    <Tooltip
      position="top"
      content={
        <div>
          Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.
        </div>
      }
      tippyProps={{
        duration: [200, 200],
        offset: 20
      }}
    >
      <div>Tippy Props Test</div>
    </Tooltip>
  );
  expect(view).toMatchSnapshot();
});
