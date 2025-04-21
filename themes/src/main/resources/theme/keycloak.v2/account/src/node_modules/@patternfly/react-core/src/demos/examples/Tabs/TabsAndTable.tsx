/* eslint-disable no-console */
import React from 'react';
import {
  Button,
  Divider,
  Drawer,
  DrawerContent,
  DrawerContentBody,
  DrawerPanelContent,
  DrawerHead,
  DrawerActions,
  DrawerCloseButton,
  DrawerPanelBody,
  Dropdown,
  Flex,
  FlexItem,
  KebabToggle,
  Label,
  LabelGroup,
  OptionsMenu,
  OptionsMenuToggle,
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuControl,
  OverflowMenuGroup,
  OverflowMenuItem,
  PageSection,
  PageSectionVariants,
  Pagination,
  PaginationVariant,
  Progress,
  ProgressSize,
  Select,
  SelectVariant,
  Tabs,
  Tab,
  TabContent,
  TabContentBody,
  TabTitleText,
  Title,
  Toolbar,
  ToolbarItem,
  ToolbarContent,
  ToolbarToggleGroup
} from '@patternfly/react-core';
import {
  TableComposable,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  IAction,
  ActionsColumn,
  CustomActionsToggleProps
} from '@patternfly/react-table';
import DashboardWrapper from '../DashboardWrapper';
import CodeIcon from '@patternfly/react-icons/dist/esm/icons/code-icon';
import CodeBranchIcon from '@patternfly/react-icons/dist/esm/icons/code-branch-icon';
import CubeIcon from '@patternfly/react-icons/dist/esm/icons/cube-icon';
import FilterIcon from '@patternfly/react-icons/dist/esm/icons/filter-icon';
import SortAmountDownIcon from '@patternfly/react-icons/dist/esm/icons/sort-amount-down-icon';

interface Repository {
  name: string;
  branches: number | null;
  prs: number | null;
  workspaces: number;
  lastCommit: string;
}

export const TablesAndTabs = () => {
  // tab properties
  const [activeTabKey, setActiveTabKey] = React.useState<number>(0);
  // Toggle currently active tab
  const handleTabClick = (tabIndex: number) => {
    setActiveTabKey(tabIndex);
  };

  // secondary tab properties
  const [secondaryActiveTabKey, setSecondaryActiveTabKey] = React.useState<number>(10);
  const handleSecondaryTabClick = (tabIndex: number) => {
    setSecondaryActiveTabKey(tabIndex);
  };

  // drawer properties
  const [isExpanded, setIsExpanded] = React.useState<boolean>(false);

  // table properties
  // In real usage, this data would come from some external source like an API via props.
  const repositories: Repository[] = [
    { name: 'Node 1', branches: 10, prs: 25, workspaces: 5, lastCommit: '2 days ago' },
    { name: 'Node 2', branches: 8, prs: 30, workspaces: 2, lastCommit: '2 days ago' },
    { name: 'Node 3', branches: 12, prs: 48, workspaces: 13, lastCommit: '30 days ago' },
    { name: 'Node 4', branches: 3, prs: 8, workspaces: 20, lastCommit: '8 days ago' },
    { name: 'Node 5', branches: 33, prs: 21, workspaces: 2, lastCommit: '26 days ago' }
  ];

  const columnNames = {
    name: 'Repositories',
    branches: 'Branches',
    prs: 'Pull requests',
    workspaces: 'Workspaces',
    lastCommit: 'Last commit'
  };

  const [selectedRepoNames, setSelectedRepoNames] = React.useState<string[]>([]);
  const setRepoSelected = (event: React.FormEvent<HTMLInputElement>, repo: Repository, isSelecting = true) => {
    setSelectedRepoNames(prevSelected => {
      const otherSelectedRepoNames = prevSelected.filter(r => r !== repo.name);
      return isSelecting ? [...otherSelectedRepoNames, repo.name] : otherSelectedRepoNames;
    });
    event.stopPropagation();
  };
  const onSelectAll = (isSelecting = true) => setSelectedRepoNames(isSelecting ? repositories.map(r => r.name) : []);
  const allRowsSelected = selectedRepoNames.length === repositories.length;
  const isRepoSelected = (repo: Repository) => selectedRepoNames.includes(repo.name);

  const [rowClicked, setRowClicked] = React.useState<string>(null);
  const isRowClicked = (repo: Repository) => rowClicked === repo.name;

  const defaultActions: IAction[] = [
    {
      title: 'Some action',
      onClick: event => {
        event.stopPropagation();
        console.log('clicked on Some action');
      }
    },
    {
      title: <a href="https://www.patternfly.org">Link action</a>,
      onClick: event => {
        event.stopPropagation();
        console.log('clicked on Link action');
      }
    },
    {
      isSeparator: true
    },
    {
      title: 'Third action',
      onClick: event => {
        event.stopPropagation();
        console.log('clicked on Third action');
      }
    }
  ];

  const customActionsToggle = (props: CustomActionsToggleProps) => (
    <KebabToggle
      isDisabled={props.isDisabled}
      onToggle={(value, event) => {
        props.onToggle(value);
        event.stopPropagation();
      }}
    />
  );

  const toolbar = (
    <Toolbar id="page-layout-table-column-management-action-toolbar-top" usePageInsets>
      <ToolbarContent>
        <ToolbarToggleGroup toggleIcon={<FilterIcon />} breakpoint="xl">
          <ToolbarItem>
            <Select
              onToggle={() => {}}
              variant={SelectVariant.single}
              aria-label="Select Input"
              placeholderText="Name"
            />
          </ToolbarItem>
        </ToolbarToggleGroup>
        <ToolbarItem>
          <OptionsMenu
            id="page-layout-table-column-management-action-toolbar-top-options-menu-toggle"
            isPlain
            menuItems={[]}
            toggle={
              <OptionsMenuToggle
                toggleTemplate={<SortAmountDownIcon aria-hidden="true" />}
                aria-label="Sort"
                hideCaret
              />
            }
          />
        </ToolbarItem>
        <OverflowMenu breakpoint="md">
          <OverflowMenuContent className="pf-u-display-none pf-u-display-block-on-lg">
            <OverflowMenuGroup groupType="button" isPersistent>
              <OverflowMenuItem isPersistent>
                <Button variant="primary">Generate</Button>
              </OverflowMenuItem>
              <OverflowMenuItem isPersistent>
                <Button variant="secondary">Deploy</Button>
              </OverflowMenuItem>
            </OverflowMenuGroup>
          </OverflowMenuContent>
          <OverflowMenuControl hasAdditionalOptions>
            <Dropdown
              onSelect={() => {}}
              toggle={<KebabToggle onToggle={() => {}} />}
              isOpen={false}
              isPlain
              dropdownItems={[]}
            />
          </OverflowMenuControl>
        </OverflowMenu>
        <ToolbarItem variant="pagination">
          <Pagination
            itemCount={36}
            widgetId="pagination-options-menu-bottom"
            page={1}
            variant={PaginationVariant.top}
            isCompact
          />
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );

  const tableComposable = (
    <TableComposable aria-label="`Composable` table">
      <Thead noWrap>
        <Tr>
          <Th
            select={{
              onSelect: (_event, isSelecting) => onSelectAll(isSelecting),
              isSelected: allRowsSelected
            }}
          />
          <Th>{columnNames.name}</Th>
          <Th>{columnNames.branches}</Th>
          <Th>{columnNames.prs}</Th>
          <Th>{columnNames.workspaces}</Th>
          <Th>{columnNames.lastCommit}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {repositories.map((repo, rowIndex) => (
          <Tr
            key={repo.name}
            onRowClick={event => {
              if ((event.target as HTMLInputElement).type !== 'checkbox') {
                setRowClicked(rowClicked === repo.name ? null : repo.name);
                setIsExpanded(!isRowClicked(repo));
              }
            }}
            isHoverable
            isRowSelected={repo.name === rowClicked}
          >
            <Td
              key={`${rowIndex}_0`}
              select={{
                rowIndex,
                onSelect: (event, isSelected) => setRepoSelected(event, repo, isSelected),
                isSelected: isRepoSelected(repo)
              }}
            />
            <Td dataLabel={columnNames.name}>
              {repo.name}
              <div>
                <a href="#">siemur/test-space</a>
              </div>
            </Td>
            <Td dataLabel={columnNames.branches}>
              <Flex>
                <FlexItem>{repo.branches}</FlexItem>
                <FlexItem>
                  <CodeBranchIcon key="icon" />
                </FlexItem>
              </Flex>
            </Td>
            <Td dataLabel={columnNames.prs}>
              <Flex>
                <FlexItem>{repo.prs}</FlexItem>
                <FlexItem>
                  <CodeIcon key="icon" />
                </FlexItem>
              </Flex>
            </Td>
            <Td dataLabel={columnNames.workspaces}>
              <Flex>
                <FlexItem>{repo.workspaces}</FlexItem>
                <FlexItem>
                  <CubeIcon key="icon" />
                </FlexItem>
              </Flex>
            </Td>
            <Td dataLabel={columnNames.lastCommit}>{repo.lastCommit}</Td>
            <Td key={`${rowIndex}_5`}>
              <ActionsColumn items={defaultActions} actionsToggle={customActionsToggle} />
            </Td>
          </Tr>
        ))}
      </Tbody>
    </TableComposable>
  );

  const panelContent = (
    <DrawerPanelContent widths={{ default: 'width_33', xl: 'width_33' }}>
      <DrawerHead>
        <DrawerActions>
          <DrawerCloseButton
            onClick={() => {
              setRowClicked(null);
              setIsExpanded(false);
            }}
          />
        </DrawerActions>
        <Flex spaceItems={{ default: 'spaceItemsSm' }} direction={{ default: 'column' }}>
          <FlexItem>
            <Title headingLevel="h2" size="lg">
              {rowClicked}
            </Title>
          </FlexItem>
          <FlexItem>
            <a href="#">siemur/test-space</a>
          </FlexItem>
        </Flex>
      </DrawerHead>
      <DrawerPanelBody hasNoPadding>
        <Tabs
          activeKey={secondaryActiveTabKey}
          onSelect={(_event, tabIndex) => handleSecondaryTabClick(Number(tabIndex))}
          isBox
          isFilled
          id="tabs-tables-secondary-tabs"
        >
          <Tab eventKey={10} title={<TabTitleText>Overview</TabTitleText>} tabContentId={`tabContent${10}`} />
          <Tab eventKey={11} title={<TabTitleText>Activity</TabTitleText>} tabContentId={`tabContent${11}`} />
        </Tabs>
      </DrawerPanelBody>
      <DrawerPanelBody>
        <TabContent
          key={10}
          eventKey={10}
          id={`tabContent${10}`}
          activeKey={secondaryActiveTabKey}
          hidden={10 !== secondaryActiveTabKey}
        >
          <TabContentBody>
            <Flex direction={{ default: 'column' }} spaceItems={{ default: 'spaceItemsLg' }}>
              <FlexItem>
                <p>
                  The content of the drawer really is up to you. It could have form fields, definition lists, text
                  lists, labels, charts, progress bars, etc. Spacing recommendation is 24px margins. You can put tabs in
                  here, and can also make the drawer scrollable.
                </p>
              </FlexItem>
              <FlexItem>
                <Progress value={33} title="Capacity" size={ProgressSize.sm} />
              </FlexItem>
              <FlexItem>
                <Progress value={66} title="Modules" size={ProgressSize.sm} />
              </FlexItem>
              <Flex direction={{ default: 'column' }}>
                <FlexItem>
                  <Title headingLevel="h3">Tags</Title>
                </FlexItem>
                <FlexItem>
                  <LabelGroup>
                    {[1, 2, 3, 4, 5].map(labelNumber => (
                      <Label variant="outline" key={`label-${labelNumber}`}>{`Tag ${labelNumber}`}</Label>
                    ))}
                  </LabelGroup>
                </FlexItem>
              </Flex>
            </Flex>
          </TabContentBody>
        </TabContent>
        <TabContent
          key={11}
          eventKey={11}
          id={`tabContent${11}`}
          activeKey={secondaryActiveTabKey}
          hidden={11 !== secondaryActiveTabKey}
        >
          <TabContentBody>Activity panel</TabContentBody>
        </TabContent>
      </DrawerPanelBody>
    </DrawerPanelContent>
  );

  const tabContent = (
    <Drawer isExpanded={isExpanded} isInline>
      <DrawerContent panelContent={panelContent}>
        <DrawerContentBody>
          {toolbar}
          <Divider />
          {tableComposable}
          <Pagination
            id="page-layout-table-column-management-action-toolbar-bottom"
            itemCount={36}
            widgetId="pagination-options-menu-bottom"
            page={1}
            variant={PaginationVariant.bottom}
          />
        </DrawerContentBody>
      </DrawerContent>
    </Drawer>
  );

  return (
    <DashboardWrapper>
      <React.Fragment>
        <PageSection variant={PageSectionVariants.light}>
          <Title headingLevel="h1" size="2xl">
            Nodes
          </Title>
        </PageSection>
        <PageSection type="tabs" variant={PageSectionVariants.light} padding={{ default: 'noPadding' }}>
          <Tabs
            activeKey={activeTabKey}
            onSelect={(_event, tabIndex) => handleTabClick(Number(tabIndex))}
            usePageInsets
            id="tabs-table-tabs-list"
          >
            <Tab eventKey={0} title={<TabTitleText>Nodes</TabTitleText>} tabContentId={`tabContent${0}`} />
            <Tab eventKey={1} title={<TabTitleText>Node connectors</TabTitleText>} tabContentId={`tabContent${1}`} />
          </Tabs>
        </PageSection>
        <PageSection variant={PageSectionVariants.light} padding={{ default: 'noPadding' }}>
          <TabContent key={0} eventKey={0} id={`tabContent${0}`} activeKey={activeTabKey} hidden={0 !== activeTabKey}>
            <TabContentBody>{tabContent}</TabContentBody>
          </TabContent>
          <TabContent key={1} eventKey={1} id={`tabContent${1}`} activeKey={activeTabKey} hidden={1 !== activeTabKey}>
            <TabContentBody>Node connectors panel</TabContentBody>
          </TabContent>
        </PageSection>
      </React.Fragment>
    </DashboardWrapper>
  );
};
