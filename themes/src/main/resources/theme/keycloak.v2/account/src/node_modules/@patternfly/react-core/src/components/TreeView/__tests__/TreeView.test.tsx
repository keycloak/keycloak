import React from 'react';
import { render } from '@testing-library/react';
import { TreeView } from '../TreeView';
import { Button } from '@patternfly/react-core';
import { FolderIcon, FolderOpenIcon } from '@patternfly/react-icons';
import { TreeViewSearch } from '../TreeViewSearch';

const options = [
  {
    name: 'ApplicationLauncher',
    id: 'AppLaunch',
    children: [
      {
        name: 'Application 1',
        id: 'App1',
        children: [
          { name: 'Settings', id: 'App1Settings' },
          { name: 'Current', id: 'App1Current' }
        ]
      },
      {
        name: 'Application 2',
        id: 'App2',
        children: [
          { name: 'Settings', id: 'App2Settings' },
          {
            name: 'Loader',
            id: 'App2Loader',
            children: [
              { name: 'Loading App 1', id: 'LoadApp1' },
              { name: 'Loading App 2', id: 'LoadApp2' },
              { name: 'Loading App 3', id: 'LoadApp3' }
            ]
          }
        ]
      }
    ],
    defaultExpanded: true
  },
  {
    name: 'Cost Management',
    id: 'Cost',
    children: [
      {
        name: 'Application 3',
        id: 'App3',
        children: [
          { name: 'Settings', id: 'App3Settings' },
          { name: 'Current', id: 'App3Current' }
        ]
      }
    ]
  },
  {
    name: 'Sources',
    id: 'Sources',
    children: [{ name: 'Application 4', id: 'App4', children: [{ name: 'Settings', id: 'App4Settings' }] }]
  },
  {
    name: 'Really really really long folder name that overflows the container it is in',
    id: 'Long',
    children: [{ name: 'Application 5', id: 'App5' }]
  }
];

const flagOptions = [
  {
    name: 'ApplicationLauncher',
    id: 'AppLaunch',
    hasCheck: true,
    icon: <FolderIcon />,
    expandedIcon: <FolderOpenIcon />,
    children: [
      {
        name: 'Application 1',
        id: 'App1',
        children: [
          { name: 'Settings', id: 'App1Settings' },
          { name: 'Current', id: 'App1Current' }
        ]
      },
      {
        name: 'Application 2',
        id: 'App2',
        hasBadge: true,
        children: [
          { name: 'Settings', id: 'App2Settings', hasCheck: true },
          {
            name: 'Loader',
            id: 'App2Loader',
            children: [
              { name: 'Loading App 1', id: 'LoadApp1' },
              { name: 'Loading App 2', id: 'LoadApp2' },
              { name: 'Loading App 3', id: 'LoadApp3' }
            ]
          }
        ]
      }
    ],
    defaultExpanded: true
  },
  {
    name: 'Cost Management',
    id: 'Cost',
    hasBadge: true,
    action: (
      <Button variant="plain" aria-label="Folder action">
        <FolderIcon />
      </Button>
    ),
    children: [
      {
        name: 'Application 3',
        id: 'App3',
        children: [
          { name: 'Settings', id: 'App3Settings' },
          { name: 'Current', id: 'App3Current' }
        ]
      }
    ]
  },
  {
    name: 'Sources',
    id: 'Sources',
    children: [{ name: 'Application 4', id: 'App4', children: [{ name: 'Settings', id: 'App4Settings' }] }]
  },
  {
    name: 'Really really really long folder name that overflows the container it is in',
    id: 'Long',
    children: [{ name: 'Application 5', id: 'App5' }]
  }
];

const active = [
  {
    name: 'Application 1',
    id: 'App1',
    children: [
      { name: 'Settings', id: 'App1Settings' },
      { name: 'Current', id: 'App1Current' }
    ]
  }
];

describe('tree view', () => {
  test('renders basic successfully', () => {
    const { asFragment } = render(<TreeView data={options} onSelect={jest.fn()} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders active successfully', () => {
    const { asFragment } = render(<TreeView data={options} activeItems={active} onSelect={jest.fn()} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders search successfully', () => {
    const { asFragment } = render(
      <TreeViewSearch onSearch={jest.fn()} id="input-search" name="search-input" aria-label="Search input example" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders toolbar successfully', () => {
    const { asFragment } = render(
      <TreeView data={options} activeItems={active} onSelect={jest.fn()} toolbar={<div>test</div>} />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders checkboxes successfully', () => {
    const { asFragment } = render(<TreeView data={options} activeItems={active} onSelect={jest.fn()} hasChecks />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders icons successfully', () => {
    const { asFragment } = render(
      <TreeView
        data={options}
        activeItems={active}
        onSelect={jest.fn()}
        icon={<FolderIcon />}
        expandedIcon={<FolderOpenIcon />}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders badges successfully', () => {
    const { asFragment } = render(<TreeView data={options} activeItems={active} onSelect={jest.fn()} hasBadges />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders individual flag options successfully', () => {
    const { asFragment } = render(<TreeView data={flagOptions} activeItems={active} onSelect={jest.fn()} />);
    expect(asFragment()).toMatchSnapshot();
  });
});

test('renders guides successfully', () => {
  const { asFragment } = render(<TreeView data={options} onSelect={jest.fn()} hasGuides={true} />);
  expect(asFragment()).toMatchSnapshot();
});

test('renders compact successfully', () => {
  const { asFragment } = render(<TreeView data={options} onSelect={jest.fn()} variant="compact" />);
  expect(asFragment()).toMatchSnapshot();
});

test('renders compact no background successfully', () => {
  const { asFragment } = render(<TreeView data={options} onSelect={jest.fn()} variant="compactNoBackground" />);
  expect(asFragment()).toMatchSnapshot();
});
