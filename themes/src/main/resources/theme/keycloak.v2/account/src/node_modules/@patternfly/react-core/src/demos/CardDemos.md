---
id: Card
section: components
---

import InfoCircleIcon from '@patternfly/react-icons/dist/js/icons/info-circle-icon';
import ArrowRightIcon from '@patternfly/react-icons/dist/js/icons/arrow-right-icon';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/js/icons/external-link-alt-icon';
import { CheckCircleIcon, ExclamationCircleIcon, ExclamationTriangleIcon, TimesCircleIcon, BellIcon } from '@patternfly/react-icons';
import { TableComposable, Thead, Tbody, Tr, Th, Td, ExpandableRowContent } from '@patternfly/react-table';
import { Chart, ChartAxis, ChartGroup, ChartVoronoiContainer, ChartStack, ChartBar, ChartTooltip, ChartDonutThreshold, ChartDonutUtilization, ChartArea, ChartContainer, ChartLabel } from '@patternfly/react-charts';
import chart_color_gold_100 from '@patternfly/react-tokens/dist/esm/chart_color_gold_100';
import chart_color_gold_300 from '@patternfly/react-tokens/dist/esm/chart_color_gold_300';
import chart_color_orange_300 from '@patternfly/react-tokens/dist/esm/chart_color_orange_300';
import chart_color_red_100 from '@patternfly/react-tokens/dist/esm/chart_color_red_100';

## Demos

### Horizontal card grid

```js
import React from 'react';
import {
  Card,
  CardHeader,
  CardActions,
  CardTitle,
  CardBody,
  CardExpandableContent,
  Level,
  LabelGroup,
  Label,
  Dropdown,
  DropdownItem,
  KebabToggle,
  Grid,
  Flex,
  List,
  ListItem,
  Button
} from '@patternfly/react-core';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';
import ArrowRightIcon from '@patternfly/react-icons/dist/esm/icons/arrow-right-icon';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-alt-icon';

class CardGridDemo extends React.Component {
  constructor(props) {
    super(props);
    this.state = { isCardExpanded: false, isDropdownOpen: false };

    this.onCardExpand = () => {
      this.setState({
        isCardExpanded: !this.state.isCardExpanded
      });
    };

    this.onActionToggle = isDropdownOpen => {
      this.setState({
        isDropdownOpen
      });
    };

    this.onActionSelect = event => {
      this.setState({
        isDropdownOpen: !this.state.isDropdownOpen
      });
    };
  }

  render() {
    const { isCardExpanded, isDropdownOpen } = this.state;
    const dropdownItems = [
      <DropdownItem key="action1" component="button">
        Action 1
      </DropdownItem>,
      <DropdownItem key="action2" component="button">
        Action 2
      </DropdownItem>,
      <DropdownItem key="disabled action3" isDisabled component="button">
        Disabled Action 3
      </DropdownItem>,
      <DropdownItem key="action4" component="button">
        Action 4
      </DropdownItem>
    ];
    return (
      <Card id="horizontal card" isExpanded={isCardExpanded}>
        <CardHeader
          onExpand={this.onCardExpand}
          toggleButtonProps={{
            id: 'toggle-button',
            'aria-label': 'Actions',
            'aria-labelledby': 'titleId toggle-button',
            'aria-expanded': isCardExpanded
          }}
        >
          <CardActions>
            <Dropdown
              onSelect={this.onActionSelect}
              toggle={<KebabToggle onToggle={this.onActionToggle} />}
              isOpen={isDropdownOpen}
              isPlain
              dropdownItems={dropdownItems}
              position="right"
            />
          </CardActions>
          {isCardExpanded && <CardTitle id="titleId">Getting Started</CardTitle>}
          {!isCardExpanded && (
            <Level hasGutter>
              <CardTitle id="titleId">Getting Started</CardTitle>
              <LabelGroup isCompact>
                <Label isCompact icon={<InfoCircleIcon />} color="blue">
                  Set up your cluster
                </Label>
                <Label isCompact icon={<InfoCircleIcon />} color="purple">
                  Guided tours
                </Label>
                <Label isCompact icon={<InfoCircleIcon />} color="green">
                  Quick starts
                </Label>
                <Label isCompact icon={<InfoCircleIcon />} color="orange">
                  Learning resources
                </Label>
              </LabelGroup>
            </Level>
          )}
        </CardHeader>
        <CardExpandableContent>
          <CardBody>
            <Grid md={6} lg={3} hasGutter>
              <Flex
                spaceItems={{ default: 'spaceItemsLg' }}
                alignItems={{ default: 'alignItemsFlexStart' }}
                direction={{ default: 'column' }}
              >
                <Flex
                  spaceItems={{ default: 'spaceItemsSm' }}
                  alignItems={{ default: 'alignItemsFlexStart' }}
                  direction={{ default: 'column' }}
                  grow={{ default: 'grow' }}
                >
                  <Label icon={<InfoCircleIcon />} color="blue">
                    Set up your cluster
                  </Label>
                  <p>Continue setting up your cluster to access all you cain in the Console</p>
                  <List isPlain>
                    <ListItem>
                      <a href="#">Add identity provider</a>
                    </ListItem>
                    <ListItem>
                      <a href="#">Configure alert receivers</a>
                    </ListItem>
                    <ListItem>
                      <a href="#">Configure default ingress certificate</a>
                    </ListItem>
                  </List>
                </Flex>
                <Button href="#" component="a" variant="link" isInline icon={<ArrowRightIcon />} iconPosition="right">
                  View all set up cluster steps
                </Button>
              </Flex>
              <Flex
                spaceItems={{ default: 'spaceItemsLg' }}
                alignItems={{ default: 'alignItemsFlexStart' }}
                direction={{ default: 'column' }}
              >
                <Flex
                  spaceItems={{ default: 'spaceItemsSm' }}
                  alignItems={{ default: 'alignItemsFlexStart' }}
                  direction={{ default: 'column' }}
                  grow={{ default: 'grow' }}
                >
                  <Label icon={<InfoCircleIcon />} color="purple">
                    Guided tours
                  </Label>
                  <p>Tour some of the key features around the console</p>
                  <List isPlain>
                    <ListItem>
                      <a href="#">Tour the console</a>
                    </ListItem>
                    <ListItem>
                      <a href="#">Getting started with Serverless</a>
                    </ListItem>
                  </List>
                </Flex>
                <Button href="#" component="a" variant="link" isInline icon={<ArrowRightIcon />} iconPosition="right">
                  View all guided tours
                </Button>
              </Flex>
              <Flex
                spaceItems={{ default: 'spaceItemsLg' }}
                alignItems={{ default: 'alignItemsFlexStart' }}
                direction={{ default: 'column' }}
              >
                <Flex
                  spaceItems={{ default: 'spaceItemsSm' }}
                  alignItems={{ default: 'alignItemsFlexStart' }}
                  direction={{ default: 'column' }}
                  grow={{ default: 'grow' }}
                >
                  <Label icon={<InfoCircleIcon />} color="green">
                    Quick starts
                  </Label>
                  <p>Get started with features using our step-by-step documentation</p>
                  <List isPlain>
                    <ListItem>
                      <a href="#">Getting started with Serverless</a>
                    </ListItem>
                    <ListItem>
                      <a href="#">Explore virtualization</a>
                    </ListItem>
                    <ListItem>
                      <a href="#">Build pipelines</a>
                    </ListItem>
                  </List>
                </Flex>
                <Button href="#" component="a" variant="link" isInline icon={<ArrowRightIcon />} iconPosition="right">
                  View all quick starts
                </Button>
              </Flex>
              <Flex
                spaceItems={{ default: 'spaceItemsLg' }}
                alignItems={{ default: 'alignItemsFlexStart' }}
                direction={{ default: 'column' }}
              >
                <Flex
                  spaceItems={{ default: 'spaceItemsSm' }}
                  alignItems={{ default: 'alignItemsFlexStart' }}
                  direction={{ default: 'column' }}
                  grow={{ default: 'grow' }}
                >
                  <Label icon={<InfoCircleIcon />} color="orange">
                    Learning resources
                  </Label>
                  <p>Learn about new features within the Console and get started with demo apps</p>
                  <List isPlain>
                    <ListItem>
                      <a href="#">See what's possible with the Explore page</a>
                    </ListItem>
                    <ListItem>
                      <a href="#">
                        OpenShift 4.5: Top Tasks
                        <ExternalLinkAltIcon />
                      </a>
                    </ListItem>
                    <ListItem>
                      <a href="#">Try a demo app</a>
                    </ListItem>
                  </List>
                </Flex>
                <Button href="#" component="a" variant="link" isInline icon={<ArrowRightIcon />} iconPosition="right">
                  View all learning resources
                </Button>
              </Flex>
            </Grid>
          </CardBody>
        </CardExpandableContent>
      </Card>
    );
  }
}
```

### Horizontal split

```js
import React from 'react';
import { Card, CardTitle, CardBody, CardFooter, Grid, GridItem, Button } from '@patternfly/react-core';

CardHorizontalSplitDemo = () => {
  return (
    <Card id="card-demo-horizontal-split-example" isFlat>
      <Grid md={6}>
        <GridItem
          style={{
            minHeight: '200px',
            backgroundPosition: 'center',
            backgroundSize: 'cover',
            backgroundImage: 'url(/assets/images/pfbg_992@2x.jpg)'
          }}
        />
        <GridItem>
          <CardTitle>Headline</CardTitle>
          <CardBody>
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse arcu purus, lobortis nec euismod eu,
            tristique ut sapien. Nullam turpis lectus, aliquet sit amet volutpat eu, semper eget quam. Maecenas in
            tempus diam. Aenean interdum velit sed massa aliquet, sit amet malesuada nulla hendrerit. Aenean non
            faucibus odio. Etiam non metus turpis. Praesent sollicitudin elit neque, id ullamcorper nibh faucibus eget.
          </CardBody>
          <CardFooter>
            <Button variant="tertiary">Call to action</Button>
          </CardFooter>
        </GridItem>
      </Grid>
    </Card>
  );
};
```

### Details card

```js
import React from 'react';
import {
  Card,
  CardTitle,
  CardBody,
  CardFooter,
  Gallery,
  Title,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Divider
} from '@patternfly/react-core';

CardDetailsDemo = () => {
  return (
    <Gallery hasGutter style={{ '--pf-l-gallery--GridTemplateColumns--min': '260px' }}>
      <Card>
        <CardTitle>
          <Title headingLevel="h2" size="xl">
            Details
          </Title>
        </CardTitle>
        <CardBody>
          <DescriptionList>
            <DescriptionListGroup>
              <DescriptionListTerm>Cluster API Address</DescriptionListTerm>
              <DescriptionListDescription>
                <a href="#">https://api1.devcluster.openshift.com</a>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Cluster ID</DescriptionListTerm>
              <DescriptionListDescription>63b97ac1-b850-41d9-8820-239becde9e86</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Provide</DescriptionListTerm>
              <DescriptionListDescription>AWS</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>OpenShift Version</DescriptionListTerm>
              <DescriptionListDescription>4.5.0.ci-2020-06-16-015028</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Update Channel</DescriptionListTerm>
              <DescriptionListDescription>stable-4.5</DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>
        </CardBody>
        <Divider />
        <CardFooter>
          <a href="#">View Settings</a>
        </CardFooter>
      </Card>
      <Card>
        <CardTitle>
          <Title headingLevel="h2" size="xl">
            Details
          </Title>
        </CardTitle>
        <CardBody>
          <DescriptionList>
            <DescriptionListGroup>
              <DescriptionListTerm>Cluster API Address</DescriptionListTerm>
              <DescriptionListDescription>
                <a href="#">https://api2.devcluster.openshift.com</a>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Cluster ID</DescriptionListTerm>
              <DescriptionListDescription>08908908-b850-41d9-8820-239becde9e86</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Provider</DescriptionListTerm>
              <DescriptionListDescription>Azure</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>OpenShift Version</DescriptionListTerm>
              <DescriptionListDescription>4.5.0.ci-2020-06-16-015026</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Update Channel</DescriptionListTerm>
              <DescriptionListDescription>stable-4.4</DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>
        </CardBody>
        <Divider />
        <CardFooter>
          <a href="#">View Settings</a>
        </CardFooter>
      </Card>
    </Gallery>
  );
};
```

### Aggregate status card

```ts
import * as React from 'react';
import {
  Card,
  CardBody,
  CardTitle,
  Divider,
  Flex,
  FlexItem,
  Gallery,
  Grid,
  GridItem,
  Stack
} from '@patternfly/react-core';
import CheckCircleIcon from '@patternfly/react-icons/dist/js/icons/check-circle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/js/icons/exclamation-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon';
import TimesCircleIcon from '@patternfly/react-icons/dist/js/icons/times-circle-icon';

const cardData = {
  iconOnly: [
    {
      title: '5 Clusters',
      content: [
        {
          icon: <CheckCircleIcon color="var(--pf-global--success-color--100)" />
        }
      ],
      layout: 'icon'
    },
    {
      title: '15 Clusters',
      content: [
        {
          icon: <ExclamationTriangleIcon color="var(--pf-global--warning-color--100)" />
        }
      ],
      layout: 'icon'
    },
    {
      title: '3 Clusters',
      content: [
        {
          icon: <TimesCircleIcon color="var(--pf-global--danger-color--100)" />
        }
      ],
      layout: 'icon'
    }
  ],
  iconWithCount: [
    {
      title: '10 Hosts',
      content: [
        {
          icon: <ExclamationCircleIcon color="var(--pf-global--success-color--100)" />,
          count: 2
        },
        {
          icon: <ExclamationTriangleIcon color="var(--pf-global--warning-color--100)" />,
          count: 1
        }
      ],
      layout: 'multiIcon'
    },
    {
      title: '50 Hosts',
      content: [
        {
          icon: <CheckCircleIcon color="var(--pf-global--success-color--100)" />,
          count: 5
        },
        {
          icon: <TimesCircleIcon color="var(--pf-global--danger-color--100)" />,
          count: 12
        }
      ],
      layout: 'multiIcon'
    },
    {
      title: '12 Hosts',
      content: [
        {
          icon: <ExclamationTriangleIcon color="var(--pf-global--warning-color--100)" />,
          count: 3
        },
        {
          icon: <TimesCircleIcon color="var(--pf-global--danger-color--100)" />,
          count: 7
        }
      ],
      layout: 'multiIcon'
    }
  ],
  withSubtitle: [
    {
      title: '13 Hosts',
      content: [
        {
          icon: <TimesCircleIcon color="var(--pf-global--danger-color--100)" />,
          status: '2 errors',
          subtitle: 'subtitle'
        },
        {
          icon: <ExclamationTriangleIcon color="var(--pf-global--warning-color--100)" />,
          status: '1 warning',
          subtitle: 'subtitle'
        }
      ],
      layout: 'withSubtitle'
    },
    {
      title: '3 Hosts',
      content: [
        {
          icon: <CheckCircleIcon color="var(--pf-global--success-color--100)" />,
          status: '2 successes',
          subtitle: 'subtitle'
        },
        {
          icon: <ExclamationTriangleIcon color="var(--pf-global--warning-color--100)" />,
          status: '3 warnings',
          subtitle: 'subtitle'
        }
      ],
      layout: 'withSubtitle'
    },
    {
      title: '50 Hosts',
      content: [
        {
          icon: <ExclamationTriangleIcon color="var(--pf-global--warning-color--100)" />,
          status: '7 warnings',
          subtitle: 'subtitle'
        },
        {
          icon: <TimesCircleIcon color="var(--pf-global--danger-color--100)" />,
          status: '1 error',
          subtitle: 'subtitle'
        }
      ],
      layout: 'withSubtitle'
    }
  ]
};

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const AggregateStatusCards: React.FunctionComponent = () => {
  const renderContent = (content, layout) => {
    if (layout === 'icon') {
      return content[0].icon;
    }
    if (layout === 'multiIcon') {
      return (
        <Flex display={{ default: 'inlineFlex' }}>
          {content.map(({ icon, count }, index: number) => (
            <React.Fragment key={index}>
              <Flex spaceItems={{ default: 'spaceItemsSm' }}>
                <FlexItem>{icon}</FlexItem>
                <FlexItem>
                  <a href="#">{count}</a>
                </FlexItem>
              </Flex>
              {content.length > 1 && index === 0 && (
                <Divider
                  key={`${index}_d`}
                  orientation={{
                    default: 'vertical'
                  }}
                />
              )}
            </React.Fragment>
          ))}
        </Flex>
      );
    }
    if (layout === 'withSubtitle') {
      return (
        <Flex justifyContent={{ default: 'justifyContentSpaceAround' }}>
          {content.map(({ icon, status, subtitle }, index) => (
            <Flex key={index}>
              <FlexItem>{icon}</FlexItem>
              <Stack>
                <a href="#">{status}</a>
                <span>{subtitle}</span>
              </Stack>
            </Flex>
          ))}
        </Flex>
      );
    }
  };
  return (
    <Grid hasGutter>
      {Object.keys(cardData).map((cardGroup, groupIndex) => {
        let galleryWidth;
        let cardAlign;
        let titleAlign;
        if (cardGroup === 'withSubtitle') {
          galleryWidth = '260px';
          cardAlign = '';
          titleAlign = 'center';
        } else {
          cardAlign = 'center';
        }
        return (
          <GridItem key={groupIndex}>
            <Gallery hasGutter style={{ '--pf-l-gallery--GridTemplateColumns--min': galleryWidth } as any}>
              {cardData[cardGroup].map(({ title, content, layout }, cardIndex) => (
                <Card style={{ textAlign: cardAlign }} key={`${groupIndex}${cardIndex}`} component="div">
                  <CardTitle style={{ textAlign: titleAlign }}>{title}</CardTitle>
                  <CardBody>{renderContent(content, layout)}</CardBody>
                </Card>
              ))}
            </Gallery>
          </GridItem>
        );
      })}
    </Grid>
  );
};
```

### Status

```ts
import * as React from 'react';
import {
  Alert,
  Card,
  CardBody,
  CardHeader,
  Divider,
  Flex,
  FlexItem,
  Grid,
  GridItem,
  Label,
  NotificationDrawer,
  NotificationDrawerBody,
  NotificationDrawerGroup,
  NotificationDrawerList,
  NotificationDrawerListItem,
  NotificationDrawerListItemBody,
  NotificationDrawerListItemHeader,
  Popover,
  Title
} from '@patternfly/react-core';
import { TableComposable, Thead, Tbody, Tr, Th, Td, ExpandableRowContent } from '@patternfly/react-table';
import BellIcon from '@patternfly/react-icons/dist/js/icons/bell-icon';
import CheckCircleIcon from '@patternfly/react-icons/dist/js/icons/check-circle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/js/icons/exclamation-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon';

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const StatusPlain: React.FunctionComponent = () => {
  const [drawerExpanded, setDrawerExpanded] = React.useState(false);
  const handleDrawerToggleClick = () => {
    setDrawerExpanded(!drawerExpanded);
  };

  const [rowsExpanded, setRowsExpanded] = React.useState([false, false, false]);
  const handleToggleExpand = (_: any, rowIndex: number) => {
    const newRowsExpanded = [...rowsExpanded];
    newRowsExpanded[rowIndex] = !rowsExpanded[rowIndex];
    setRowsExpanded(newRowsExpanded);
  };

  const header = (
    <CardHeader>
      <Title headingLevel="h2" size="lg">
        Status
      </Title>
    </CardHeader>
  );

  const columns = ['Components', 'Response Rate'];

  const rows = [
    {
      content: ['API Servers', '20%'],
      child: (
        <Alert
          title="This is a critical alert that can be associated with the control panel."
          variant="danger"
          isInline
        ></Alert>
      )
    },
    {
      content: ['Controller Managers', '100%'],
      child: (
        <Alert
          title="This is a critical alert that can be associated with the control panel."
          variant="danger"
          isInline
        ></Alert>
      )
    },
    {
      content: ['etcd', '91%'],
      child: (
        <Alert
          title="This is a critical alert that can be associated with the control panel."
          variant="danger"
          isInline
        ></Alert>
      )
    }
  ];

  const popoverBodyContent = (
    <>
      <div>
        Components of the Control Panel are responsible for maintaining and reconciling the state of the cluster.
      </div>
      <TableComposable variant="compact">
        <Thead>
          <Tr>
            <Th />
            {columns.map((column, columnIndex) => (
              <Th key={columnIndex} modifier="fitContent">
                {column}
              </Th>
            ))}
          </Tr>
        </Thead>
        {rows.map((row, rowIndex) => {
          const parentRow = (
            <Tr key={rowIndex}>
              <Td
                key={`${rowIndex}_0`}
                expand={row.child && { isExpanded: rowsExpanded[rowIndex], rowIndex, onToggle: handleToggleExpand }}
              />
              {row.content.map((cell, cellIndex) => (
                <Td key={`${rowIndex}_${cellIndex}`} dataLabel={columns[cellIndex]} modifier="fitContent">
                  {cell}
                </Td>
              ))}
            </Tr>
          );
          const childRow = row.child ? (
            <Tr key={`${rowIndex}_child`} isExpanded={rowsExpanded[rowIndex]}>
              <Td key={`${rowIndex}_1`} colSpan={3} dataLabel={`${rowIndex}_child`}>
                <ExpandableRowContent>{row.child}</ExpandableRowContent>
              </Td>
            </Tr>
          ) : null;
          return (
            <Tbody key={rowIndex} isExpanded={rowsExpanded[rowIndex]}>
              {parentRow}
              {childRow}
            </Tbody>
          );
        })}
      </TableComposable>
    </>
  );

  const body = (
    <CardBody>
      <Grid hasGutter sm={6} lg={3}>
        <GridItem>
          <Flex spaceItems={{ default: 'spaceItemsSm' }}>
            <FlexItem>
              <CheckCircleIcon color="var(--pf-global--success-color--100)" />
            </FlexItem>
            <FlexItem>
              <span>Cluster</span>
            </FlexItem>
          </Flex>
        </GridItem>
        <GridItem>
          <Flex spaceItems={{ default: 'spaceItemsSm' }}>
            <FlexItem>
              <ExclamationCircleIcon color="var(--pf-global--danger-color--100)" />
            </FlexItem>
            <FlexItem>
              <Popover headerContent="Control Panel Status" bodyContent={popoverBodyContent} minWidth="400px">
                <a href="#" onClick={e => e.preventDefault()}>
                  Control Panel
                </a>
              </Popover>
            </FlexItem>
          </Flex>
        </GridItem>
        <GridItem>
          <Flex spaceItems={{ default: 'spaceItemsSm' }}>
            <FlexItem>
              <ExclamationCircleIcon color="var(--pf-global--danger-color--100)" />
            </FlexItem>
            <Flex direction={{ default: 'column' }} spaceItems={{ default: 'spaceItemsNone' }}>
              <FlexItem>
                <a href="#">Operators</a>
              </FlexItem>
              <FlexItem>
                <span style={{ color: 'var(--pf-global--Color--400)' }}>1 degraded</span>
              </FlexItem>
            </Flex>
          </Flex>
        </GridItem>
        <GridItem>
          <Flex spaceItems={{ default: 'spaceItemsSm' }}>
            <FlexItem>
              <CheckCircleIcon color="var(--pf-global--success-color--100)" />
            </FlexItem>
            <Flex direction={{ default: 'column' }} spaceItems={{ default: 'spaceItemsNone' }}>
              <FlexItem>
                <a href="#">Image Vulnerabilities</a>
              </FlexItem>
              <FlexItem>
                <span style={{ color: '#8a8d90' }}>0 vulnerabilities</span>
              </FlexItem>
            </Flex>
          </Flex>
        </GridItem>
      </Grid>
    </CardBody>
  );

  const drawerTitle = (
    <Flex spaceItems={{ default: 'spaceItemsSm' }}>
      <FlexItem spacer={{ default: 'spacerMd' }}>
        <span>Notifications</span>
      </FlexItem>
      <Label color="red" icon={<ExclamationCircleIcon />}>
        1
      </Label>
      <Label color="orange" icon={<ExclamationTriangleIcon />}>
        3
      </Label>
      <Label color="green" icon={<CheckCircleIcon />}>
        3
      </Label>
      <Label color="blue" icon={<ExclamationCircleIcon />}>
        3
      </Label>
      <Label color="green" icon={<BellIcon />}>
        3
      </Label>
    </Flex>
  );

  const drawer = (
    <NotificationDrawer>
      <NotificationDrawerBody>
        <NotificationDrawerGroup
          count={0}
          onExpand={handleDrawerToggleClick}
          isExpanded={drawerExpanded}
          title={drawerTitle}
        >
          <NotificationDrawerList isHidden={!drawerExpanded}>
            <NotificationDrawerListItem variant="danger">
              <NotificationDrawerListItemHeader variant="danger" title="Critical alert regarding control plane" />
              <NotificationDrawerListItemBody>
                This is a long description to show how the title will wrap if it is long and wraps to multiple lines.
              </NotificationDrawerListItemBody>
            </NotificationDrawerListItem>
            <NotificationDrawerListItem variant="warning">
              <NotificationDrawerListItemHeader variant="warning" title="Warning alert" />
              <NotificationDrawerListItemBody>
                This is a warning notification description.
              </NotificationDrawerListItemBody>
            </NotificationDrawerListItem>
          </NotificationDrawerList>
        </NotificationDrawerGroup>
      </NotificationDrawerBody>
    </NotificationDrawer>
  );

  return (
    <Card>
      {header}
      {body}
      <Divider />
      {drawer}
    </Card>
  );
};
```

### Status Tabbed

```ts
import * as React from 'react';
import {
  Card,
  CardBody,
  CardHeader,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Flex,
  FlexItem,
  Grid,
  GridItem,
  Spinner,
  Tab,
  TabContent,
  Tabs,
  TabTitleText,
  Title
} from '@patternfly/react-core';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';

const descriptionListData = [
  {
    status: 'Running',
    resourceName: 'Resource name that is long and can wrap',
    detail: '121 Systems',
    icon: <CheckCircleIcon />
  },
  {
    status: 'Ready',
    resourceName: 'Resource name that is long and can wrap',
    detail: '123 Systems',
    icon: <ExclamationCircleIcon />
  },
  {
    status: 'Running',
    resourceName: 'Resource name that is long and can wrap',
    detail: '122 Systems',
    icon: <CheckCircleIcon />
  },
  {
    status: 'Ready',
    resourceName: 'Resource name that is long and can wrap',
    detail: '124 Systems',
    icon: <ExclamationCircleIcon />
  }
];

const Status: React.FunctionComponent = () => {
  const [activeTabKey, setActiveTabKey] = React.useState(0);
  const handleTabClick = (event: React.MouseEvent, tabIndex: number) => {
    setActiveTabKey(tabIndex);
  };

  const tabContent = (
    <DescriptionList isHorizontal columnModifier={{ lg: '2Col' }}>
      {descriptionListData.map(({ status, resourceName, detail, icon }, index) => (
        <DescriptionListGroup key={index}>
          <DescriptionListTerm>
            <Flex>
              <FlexItem>{icon}</FlexItem>
              <FlexItem>
                <Title headingLevel="h3" size="md">
                  {status}
                </Title>
              </FlexItem>
            </Flex>
          </DescriptionListTerm>
          <DescriptionListDescription>
            <a href="#">{resourceName}</a>
            <div>{detail}</div>
          </DescriptionListDescription>
        </DescriptionListGroup>
      ))}
    </DescriptionList>
  );

  return (
    <>
      <Card>
        <CardHeader>
          <Title headingLevel="h2" size="lg">
            Status
          </Title>
        </CardHeader>
        <CardBody>
          <Tabs isFilled id="status-tabs" activeKey={activeTabKey} onSelect={handleTabClick}>
            {[1, 2, 3].map((tab, tabIndex) => (
              <Tab
                key={tabIndex}
                eventKey={tabIndex}
                title={<TabTitleText>{`Object ${tabIndex + 1}`}</TabTitleText>}
                tabContentId={`tabContent${tabIndex}`}
              />
            ))}
          </Tabs>
        </CardBody>
        <CardBody>
          {[1, 2, 3].map((tab, tabIndex) => (
            <TabContent
              key={tabIndex}
              eventKey={tabIndex}
              id={`tabContent${tabIndex}`}
              activeKey={activeTabKey}
              hidden={tabIndex !== activeTabKey}
            >
              {tabContent}
            </TabContent>
          ))}
        </CardBody>
      </Card>
    </>
  );
};
```

### Utilization card 1

```ts
import React from 'react';
import {
  Card,
  CardTitle,
  CardBody,
  CardFooter,
  Title,
  Gallery,
  GalleryItem,
  Flex,
  FlexItem,
  Stack,
  StackItem,
  Divider
} from '@patternfly/react-core';
import { ChartArea, ChartContainer, ChartGroup, ChartLabel, ChartVoronoiContainer } from '@patternfly/react-charts';

<Gallery hasGutter minWidths={{ default: '360px' }}>
  <GalleryItem>
    <Card id="utilization-card-1" component="div">
      <CardTitle>
        <Title headingLevel="h2" size="lg">
          Top Utilized Clusters
        </Title>
      </CardTitle>
      <CardBody>
        <Flex direction={{ default: 'column' }}>
          <FlexItem>
            <Stack>
              <b>Cluster-1204</b>
              <span>27.3 cores available</span>
            </Stack>
          </FlexItem>
          <FlexItem>
            <ChartGroup
              ariaDesc="Mock average cluster utilization"
              ariaTitle="Mock cluster sparkline chart"
              containerComponent={
                <ChartVoronoiContainer labels={({ datum }) => `${datum.name}: ${datum.y}`} constrainToVisibleArea />
              }
              height={100}
              maxDomain={{ y: 9 }}
              padding={0}
              width={400}
            >
              <ChartArea
                data={[
                  { name: 'Cluster', x: '2015', y: 7 },
                  { name: 'Cluster', x: '2016', y: 6 },
                  { name: 'Cluster', x: '2017', y: 8 },
                  { name: 'Cluster', x: '2018', y: 3 },
                  { name: 'Cluster', x: '2019', y: 4 },
                  { name: 'Cluster', x: '2020', y: 1 },
                  { name: 'Cluster', x: '2021', y: 0 }
                ]}
              />
            </ChartGroup>
          </FlexItem>
          <FlexItem>
            <a href="#">View details</a>
          </FlexItem>
        </Flex>
      </CardBody>
      <CardBody>
        <Flex direction={{ default: 'column' }}>
          <FlexItem>
            <Stack>
              <b>Abcdef-1204</b>
              <span>50.6 cores available</span>
            </Stack>
          </FlexItem>
          <FlexItem>
            <ChartGroup
              ariaDesc="Mock average cluster utilization"
              ariaTitle="Mock cluster sparkline chart"
              containerComponent={
                <ChartVoronoiContainer labels={({ datum }) => `${datum.name}: ${datum.y}`} constrainToVisibleArea />
              }
              height={100}
              maxDomain={{ y: 9 }}
              padding={0}
              width={400}
            >
              <ChartArea
                data={[
                  { name: 'Cluster', x: '2015', y: 7 },
                  { name: 'Cluster', x: '2016', y: 6 },
                  { name: 'Cluster', x: '2017', y: 8 },
                  { name: 'Cluster', x: '2018', y: 3 },
                  { name: 'Cluster', x: '2019', y: 4 },
                  { name: 'Cluster', x: '2020', y: 1 },
                  { name: 'Cluster', x: '2021', y: 0 }
                ]}
              />
            </ChartGroup>
          </FlexItem>
          <FlexItem>
            <a href="#">View details</a>
          </FlexItem>
        </Flex>
      </CardBody>
      <Divider />
      <CardFooter>
        <a href="#">View all clusters</a>
      </CardFooter>
    </Card>
  </GalleryItem>
</Gallery>;
```

### Utilization card 2

```ts
import React from 'react';
import {
  Card,
  CardTitle,
  CardBody,
  CardFooter,
  Title,
  Gallery,
  GalleryItem,
  Flex,
  FlexItem,
  Stack,
  StackItem,
  Divider
} from '@patternfly/react-core';
import { ChartArea, ChartContainer, ChartGroup, ChartLabel, ChartVoronoiContainer } from '@patternfly/react-charts';

<Gallery hasGutter minWidths={{ default: '360px' }}>
  <GalleryItem>
    <Card id="utilization-card-2" component="div">
      <CardTitle>
        <Title headingLevel="h2" size="lg">
          Top Utilized Clusters
        </Title>
      </CardTitle>
      <CardBody>
        <Flex direction={{ default: 'column' }}>
          <FlexItem>
            <Stack>
              <a href="#">Cluster-1204</a>
              <span>27.3 cores available</span>
            </Stack>
          </FlexItem>
          <FlexItem>
            <ChartGroup
              ariaDesc="Mock average cluster utilization"
              ariaTitle="Mock cluster sparkline chart"
              containerComponent={
                <ChartVoronoiContainer labels={({ datum }) => `${datum.name}: ${datum.y}`} constrainToVisibleArea />
              }
              height={100}
              maxDomain={{ y: 9 }}
              padding={0}
              width={400}
            >
              <ChartArea
                data={[
                  { name: 'Cluster', x: '2015', y: 7 },
                  { name: 'Cluster', x: '2016', y: 6 },
                  { name: 'Cluster', x: '2017', y: 8 },
                  { name: 'Cluster', x: '2018', y: 3 },
                  { name: 'Cluster', x: '2019', y: 4 },
                  { name: 'Cluster', x: '2020', y: 1 },
                  { name: 'Cluster', x: '2021', y: 0 }
                ]}
              />
            </ChartGroup>
          </FlexItem>
        </Flex>
      </CardBody>
      <CardBody>
        <Flex direction={{ default: 'column' }}>
          <FlexItem>
            <Stack>
              <a href="#">Abcdef-1204</a>
              <span>50.6 cores available</span>
            </Stack>
          </FlexItem>
          <FlexItem>
            <ChartGroup
              ariaDesc="Mock average cluster utilization"
              ariaTitle="Mock cluster sparkline chart"
              containerComponent={
                <ChartVoronoiContainer labels={({ datum }) => `${datum.name}: ${datum.y}`} constrainToVisibleArea />
              }
              height={100}
              maxDomain={{ y: 9 }}
              padding={0}
              width={400}
            >
              <ChartArea
                data={[
                  { name: 'Cluster', x: '2015', y: 7 },
                  { name: 'Cluster', x: '2016', y: 6 },
                  { name: 'Cluster', x: '2017', y: 8 },
                  { name: 'Cluster', x: '2018', y: 3 },
                  { name: 'Cluster', x: '2019', y: 4 },
                  { name: 'Cluster', x: '2020', y: 1 },
                  { name: 'Cluster', x: '2021', y: 0 }
                ]}
              />
            </ChartGroup>
          </FlexItem>
        </Flex>
      </CardBody>
      <Divider />
      <CardFooter>
        <a href="#">View all clusters</a>
      </CardFooter>
    </Card>
  </GalleryItem>
</Gallery>;
```

### Utilization card 3

```ts
import React from 'react';
import {
  Card,
  CardTitle,
  CardHeader,
  CardActions,
  CardBody,
  CardFooter,
  Title,
  Gallery,
  GalleryItem,
  Flex,
  FlexItem,
  Stack,
  StackItem,
  Divider,
  Select,
  SelectOption
} from '@patternfly/react-core';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
import { Chart, ChartStack, ChartBar, ChartTooltip } from '@patternfly/react-charts';
import chart_color_gold_100 from '@patternfly/react-tokens/dist/esm/chart_color_gold_100';
import chart_color_gold_300 from '@patternfly/react-tokens/dist/esm/chart_color_gold_300';
import chart_color_orange_300 from '@patternfly/react-tokens/dist/esm/chart_color_orange_300';
import chart_color_red_100 from '@patternfly/react-tokens/dist/esm/chart_color_red_100';

const UtilizationCard3: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const selectItems = [
    <SelectOption value="Last hour" key="option1" />,
    <SelectOption value="Last 6 hours" key="option2" />,
    <SelectOption value="Last 24 hours" key="option3" />,
    <SelectOption value="Last 7 days" key="option4" />
  ];

  return (
    <React.Fragment>
      <b>Note:</b> Custom CSS is used in this demo to align the card title and select toggle text to{' '}
      <code>baseline</code> alignment.
      <br />
      <br />
      <Gallery hasGutter minWidths={{ default: '360px' }}>
        <GalleryItem>
          <Card id="utilization-card-1" component="div">
            <CardHeader className="pf-u-align-items-flex-start">
              <CardTitle>
                <Title headingLevel="h2" size="lg" style={{ paddingTop: '3px' }}>
                  Recommendations
                </Title>
              </CardTitle>
              <CardActions hasNoOffset>
                <Select
                  onSelect={() => setIsOpen(!isOpen)}
                  onToggle={() => setIsOpen(!isOpen)}
                  isOpen={isOpen}
                  placeholderText="Filter"
                  position="right"
                  isPlain
                >
                  {selectItems}
                </Select>
              </CardActions>
            </CardHeader>
            <CardBody>
              <Flex direction={{ default: 'column' }}>
                <FlexItem>
                  <span>System</span>
                </FlexItem>
                <Flex>
                  <ExclamationCircleIcon className="pf-u-danger-color-100" aria-hidden />
                  <a href="#">25 incidents detected</a>
                </Flex>
                <FlexItem>
                  <Chart
                    ariaDesc="Mock incidents chart"
                    ariaTitle="Mock stack chart"
                    domainPadding={{ x: [30, 25] }}
                    legendData={[
                      { name: 'Low', symbol: { fill: chart_color_gold_100.value } },
                      { name: 'Important', symbol: { fill: chart_color_gold_300.value } },
                      { name: 'Moderate', symbol: { fill: chart_color_orange_300.value } },
                      { name: 'Critical', symbol: { fill: chart_color_red_100.value } }
                    ]}
                    legendPosition="bottom-left"
                    height={50}
                    padding={{
                      bottom: 40,
                      left: 0,
                      right: 0,
                      top: 0
                    }}
                    width={350}
                    showAxis={false}
                  >
                    <ChartStack
                      horizontal
                      colorScale={[
                        chart_color_gold_100.value,
                        chart_color_gold_300.value,
                        chart_color_orange_300.value,
                        chart_color_red_100.value
                      ]}
                    >
                      <ChartBar
                        data={[
                          {
                            name: 'Low',
                            x: 'Cluster A',
                            y: 6,
                            label: 'Low: 6'
                          }
                        ]}
                        labelComponent={<ChartTooltip constrainToVisibleArea />}
                      />
                      <ChartBar
                        data={[
                          {
                            name: 'Important',
                            x: 'Cluster A',
                            y: 2,
                            label: 'Important: 2'
                          }
                        ]}
                        labelComponent={<ChartTooltip constrainToVisibleArea />}
                      />
                      <ChartBar
                        data={[
                          {
                            name: 'Moderate',
                            x: 'Cluster A',
                            y: 4,
                            label: 'Moderate: 4'
                          }
                        ]}
                        labelComponent={<ChartTooltip constrainToVisibleArea />}
                      />
                      <ChartBar
                        data={[
                          {
                            name: 'Critical',
                            x: 'Cluster A',
                            y: 2,
                            label: 'Critical: 2'
                          }
                        ]}
                        labelComponent={<ChartTooltip constrainToVisibleArea />}
                      />
                    </ChartStack>
                  </Chart>
                </FlexItem>
              </Flex>
            </CardBody>
            <CardFooter>
              <a href="#">See details</a>
            </CardFooter>
          </Card>
        </GalleryItem>
      </Gallery>
    </React.Fragment>
  );
};
```

### Utilization card 4

```ts
import React from 'react';
import { Card, CardTitle, CardBody, CardFooter, Title, Gallery, GalleryItem } from '@patternfly/react-core';
import { ChartDonutThreshold, ChartDonutUtilization } from '@patternfly/react-charts';

<Gallery hasGutter minWidths={{ default: '360px' }}>
  <GalleryItem>
    <Card id="utilization-card-1" component="div">
      <CardTitle>
        <Title headingLevel="h2" size="lg">
          CPU Usage
        </Title>
      </CardTitle>
      <CardBody>
        <ChartDonutThreshold
          ariaDesc="Mock storage capacity"
          ariaTitle="Mock donut utilization chart"
          constrainToVisibleArea={true}
          data={[
            { x: 'Warning at 60%', y: 60 },
            { x: 'Danger at 90%', y: 90 }
          ]}
          height={200}
          labels={({ datum }) => (datum.x ? datum.x : null)}
          padding={{
            bottom: 0,
            left: 10,
            right: 150,
            top: 0
          }}
          width={350}
        >
          <ChartDonutUtilization
            data={{ x: 'Storage capacity', y: 80 }}
            labels={({ datum }) => (datum.x ? `${datum.x}: ${datum.y}%` : null)}
            legendData={[{ name: `Capacity: 80%` }, { name: 'Warning at 60%' }, { name: 'Danger at 90%' }]}
            legendOrientation="vertical"
            title="80%"
            subTitle="of 100 GBps"
            thresholds={[{ value: 60 }, { value: 90 }]}
          />
        </ChartDonutThreshold>{' '}
      </CardBody>
      <CardFooter>
        <a href="#">See details</a>
      </CardFooter>
    </Card>
  </GalleryItem>
</Gallery>;
```

### Nested cards

```js
import React from 'react';
import {
  Card,
  CardHeader,
  CardTitle,
  CardBody,
  CardExpandableContent,
  Checkbox,
  Title,
  Divider,
  Grid,
  GridItem,
  Flex,
  FlexItem
} from '@patternfly/react-core';
import { ChartArea, ChartGroup, ChartVoronoiContainer } from '@patternfly/react-charts';

CardNestedDemo = () => {
  const [isCardExpanded1, onCardExpand1] = React.useState(true);
  const [isCardExpanded2, onCardExpand2] = React.useState(false);
  const [isCardExpanded3, onCardExpand3] = React.useState(false);
  const [isToggleOnRight, onCheckClick] = React.useState(false);

  return (
    <>
      <div style={{ marginBottom: '12px' }}>
        <Checkbox
          id="isToggleRightAligned"
          key="isToggleRightAligned"
          label="Align expand toggle on right"
          isChecked={isToggleOnRight}
          onChange={checked => onCheckClick(!isToggleOnRight)}
        />
      </div>
      <Card id="nested-cards">
        <CardHeader>
          <CardTitle id="nested-cards-toggle-title">
            <Title headingLevel="h2" size="lg">
              {' '}
              Hardware Monitor{' '}
            </Title>
          </CardTitle>
        </CardHeader>
        <Card id="nested-cards-toggle-group-1" isPlain isExpanded={isCardExpanded1}>
          <CardHeader
            onExpand={() => onCardExpand1(!isCardExpanded1)}
            isToggleRightAligned={isToggleOnRight}
            toggleButtonProps={{
              id: 'toggle-button-1',
              'aria-label': 'Details',
              'aria-labelledby': 'nested-cards-toggle-group-1-title toggle-button-1',
              'aria-expanded': isCardExpanded1
            }}
          >
            <CardTitle id="nested-cards-toggle-group-1-title">
              <span className="pf-u-font-weight-light">CPU 1</span>
            </CardTitle>
          </CardHeader>
          <CardExpandableContent>
            <CardBody>
              <Grid hasGutter>
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Temperature</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>64C</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">100C</div>
                          <div className="pf-l-flex__item">50C</div>
                          <div className="pf-l-flex__item">0C</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU temperature"
                          ariaTitle="Mock CPU temperature sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 100 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 25 },
                              { name: 'Temp', x: '2', y: 40 },
                              { name: 'Temp', x: '3', y: 20 },
                              { name: 'Temp', x: '4', y: 60 },
                              { name: 'Temp', x: '5', y: 20 },
                              { name: 'Temp', x: '6', y: 41 },
                              { name: 'Temp', x: '7', y: 45 },
                              { name: 'Temp', x: '8', y: 41 },
                              { name: 'Temp', x: '9', y: 62 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
                <Divider className="pf-u-hidden-on-md" />
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Speed</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>2.3Ghz</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">3.6Ghz</div>
                          <div className="pf-l-flex__item">1.5Ghz</div>
                          <div className="pf-l-flex__item">0GHZ</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU speed"
                          ariaTitle="Mock CPU speed sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 3.6 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 0.9 },
                              { name: 'Temp', x: '2', y: 1.44 },
                              { name: 'Temp', x: '3', y: 0.72 },
                              { name: 'Temp', x: '4', y: 2.16 },
                              { name: 'Temp', x: '5', y: 0.72 },
                              { name: 'Temp', x: '6', y: 1.48 },
                              { name: 'Temp', x: '7', y: 1.62 },
                              { name: 'Temp', x: '8', y: 1.48 },
                              { name: 'Temp', x: '9', y: 2.23 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
              </Grid>
            </CardBody>
          </CardExpandableContent>
        </Card>
        <Card id="nested-cards-toggle-group-2" isPlain isExpanded={isCardExpanded3}>
          <CardHeader
            onExpand={() => onCardExpand3(!isCardExpanded3)}
            isToggleRightAligned={isToggleOnRight}
            toggleButtonProps={{
              id: 'toggle-button-2',
              'aria-label': 'Details',
              'aria-labelledby': 'nested-cards-toggle-group-2-title toggle-button-2',
              'aria-expanded': isCardExpanded3
            }}
          >
            <CardTitle id="nested-cards-toggle-group-2-title">
              <span className="pf-u-font-weight-light">CPU 2</span>
            </CardTitle>
          </CardHeader>
          <CardExpandableContent>
            <CardBody>
              <Grid hasGutter>
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Temperature</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>64C</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">100C</div>
                          <div className="pf-l-flex__item">50C</div>
                          <div className="pf-l-flex__item">0C</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU temperature"
                          ariaTitle="Mock CPU temperature sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 100 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 25 },
                              { name: 'Temp', x: '2', y: 40 },
                              { name: 'Temp', x: '3', y: 20 },
                              { name: 'Temp', x: '4', y: 60 },
                              { name: 'Temp', x: '5', y: 20 },
                              { name: 'Temp', x: '6', y: 41 },
                              { name: 'Temp', x: '7', y: 45 },
                              { name: 'Temp', x: '8', y: 41 },
                              { name: 'Temp', x: '9', y: 62 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
                <Divider className="pf-u-hidden-on-md" />
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Speed</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>2.3Ghz</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">3.6Ghz</div>
                          <div className="pf-l-flex__item">1.5Ghz</div>
                          <div className="pf-l-flex__item">0GHZ</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU speed"
                          ariaTitle="Mock CPU speed sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 3.6 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 0.9 },
                              { name: 'Temp', x: '2', y: 1.44 },
                              { name: 'Temp', x: '3', y: 0.72 },
                              { name: 'Temp', x: '4', y: 2.16 },
                              { name: 'Temp', x: '5', y: 0.72 },
                              { name: 'Temp', x: '6', y: 1.48 },
                              { name: 'Temp', x: '7', y: 1.62 },
                              { name: 'Temp', x: '8', y: 1.48 },
                              { name: 'Temp', x: '9', y: 2.23 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
              </Grid>
            </CardBody>
          </CardExpandableContent>
        </Card>
        <Card id="nested-cards-toggle-group-3" isPlain isExpanded={isCardExpanded2}>
          <CardHeader
            onExpand={() => onCardExpand2(!isCardExpanded2)}
            isToggleRightAligned={isToggleOnRight}
            toggleButtonProps={{
              id: 'toggle-button-3',
              'aria-label': 'Details',
              'aria-labelledby': 'nested-cards-toggle-group-3-title toggle-button-3',
              'aria-expanded': isCardExpanded2
            }}
          >
            <CardTitle id="nested-cards-toggle-group-3-title">
              <span className="pf-u-font-weight-light">CPU 3</span>
            </CardTitle>
          </CardHeader>
          <CardExpandableContent>
            <CardBody>
              <Grid hasGutter>
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Temperature</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>64C</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">100C</div>
                          <div className="pf-l-flex__item">50C</div>
                          <div className="pf-l-flex__item">0C</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU temperature"
                          ariaTitle="Mock CPU temperature sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 100 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 25 },
                              { name: 'Temp', x: '2', y: 40 },
                              { name: 'Temp', x: '3', y: 20 },
                              { name: 'Temp', x: '4', y: 60 },
                              { name: 'Temp', x: '5', y: 20 },
                              { name: 'Temp', x: '6', y: 41 },
                              { name: 'Temp', x: '7', y: 45 },
                              { name: 'Temp', x: '8', y: 41 },
                              { name: 'Temp', x: '9', y: 62 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
                <Divider className="pf-u-hidden-on-md" />
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Speed</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>2.3Ghz</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">3.6Ghz</div>
                          <div className="pf-l-flex__item">1.5Ghz</div>
                          <div className="pf-l-flex__item">0GHZ</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU speed"
                          ariaTitle="Mock CPU speed sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 3.6 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 0.9 },
                              { name: 'Temp', x: '2', y: 1.44 },
                              { name: 'Temp', x: '3', y: 0.72 },
                              { name: 'Temp', x: '4', y: 2.16 },
                              { name: 'Temp', x: '5', y: 0.72 },
                              { name: 'Temp', x: '6', y: 1.48 },
                              { name: 'Temp', x: '7', y: 1.62 },
                              { name: 'Temp', x: '8', y: 1.48 },
                              { name: 'Temp', x: '9', y: 2.23 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
              </Grid>
            </CardBody>
          </CardExpandableContent>
        </Card>
      </Card>
    </>
  );
};
```

### With accordion

```ts
import React from 'react';
import {
  Card,
  CardTitle,
  CardBody,
  CardHeader,
  Title,
  Accordion,
  AccordionItem,
  AccordionToggle,
  AccordionContent,
  Divider,
  Grid,
  GridItem,
  Flex,
  FlexItem
} from '@patternfly/react-core';
import { ChartArea, ChartContainer, ChartGroup, ChartLabel, ChartVoronoiContainer } from '@patternfly/react-charts';

const AccordionCard: React.FunctionComponent = () => {
  const [openCPU, setOpenCPU] = React.useState('cpu1');

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <Title headingLevel="h2" size="lg">
            Hardware Monitor
          </Title>
        </CardTitle>
      </CardHeader>
      <CardBody>
        <Accordion>
          <AccordionItem>
            <AccordionToggle
              id="cpu1-toggle"
              onClick={() => {
                if (openCPU === 'cpu1') {
                  setOpenCPU(null);
                } else {
                  setOpenCPU('cpu1');
                }
              }}
              isExpanded={openCPU === 'cpu1'}
            >
              CPU 1
            </AccordionToggle>
            <AccordionContent isHidden={openCPU !== 'cpu1'}>
              <Grid hasGutter>
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Temperature</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>64C</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">100C</div>
                          <div className="pf-l-flex__item">50C</div>
                          <div className="pf-l-flex__item">0C</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU temperature"
                          ariaTitle="Mock CPU temperature sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 100 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 25 },
                              { name: 'Temp', x: '2', y: 40 },
                              { name: 'Temp', x: '3', y: 20 },
                              { name: 'Temp', x: '4', y: 60 },
                              { name: 'Temp', x: '5', y: 20 },
                              { name: 'Temp', x: '6', y: 41 },
                              { name: 'Temp', x: '7', y: 45 },
                              { name: 'Temp', x: '8', y: 41 },
                              { name: 'Temp', x: '9', y: 62 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
                <Divider className="pf-u-hidden-on-md" />
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Speed</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>2.3Ghz</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">3.6Ghz</div>
                          <div className="pf-l-flex__item">1.5Ghz</div>
                          <div className="pf-l-flex__item">0GHZ</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU speed"
                          ariaTitle="Mock CPU speed sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 3.6 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 0.9 },
                              { name: 'Temp', x: '2', y: 1.44 },
                              { name: 'Temp', x: '3', y: 0.72 },
                              { name: 'Temp', x: '4', y: 2.16 },
                              { name: 'Temp', x: '5', y: 0.72 },
                              { name: 'Temp', x: '6', y: 1.48 },
                              { name: 'Temp', x: '7', y: 1.62 },
                              { name: 'Temp', x: '8', y: 1.48 },
                              { name: 'Temp', x: '9', y: 2.23 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
              </Grid>
            </AccordionContent>
          </AccordionItem>
          <AccordionItem>
            <AccordionToggle
              id="cpu2-toggle"
              onClick={() => {
                if (openCPU === 'cpu2') {
                  setOpenCPU(null);
                } else {
                  setOpenCPU('cpu2');
                }
              }}
              isExpanded={openCPU === 'cpu2'}
            >
              CPU 2
            </AccordionToggle>
            <AccordionContent isHidden={openCPU !== 'cpu2'}>
              <Grid hasGutter>
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Temperature</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>64C</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">100C</div>
                          <div className="pf-l-flex__item">50C</div>
                          <div className="pf-l-flex__item">0C</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU temperature"
                          ariaTitle="Mock CPU temperature sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 100 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 25 },
                              { name: 'Temp', x: '2', y: 40 },
                              { name: 'Temp', x: '3', y: 20 },
                              { name: 'Temp', x: '4', y: 60 },
                              { name: 'Temp', x: '5', y: 20 },
                              { name: 'Temp', x: '6', y: 41 },
                              { name: 'Temp', x: '7', y: 45 },
                              { name: 'Temp', x: '8', y: 41 },
                              { name: 'Temp', x: '9', y: 62 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
                <Divider className="pf-u-hidden-on-md" />
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Speed</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>2.3Ghz</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">3.6Ghz</div>
                          <div className="pf-l-flex__item">1.5Ghz</div>
                          <div className="pf-l-flex__item">0GHZ</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU speed"
                          ariaTitle="Mock CPU speed sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 3.6 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 0.9 },
                              { name: 'Temp', x: '2', y: 1.44 },
                              { name: 'Temp', x: '3', y: 0.72 },
                              { name: 'Temp', x: '4', y: 2.16 },
                              { name: 'Temp', x: '5', y: 0.72 },
                              { name: 'Temp', x: '6', y: 1.48 },
                              { name: 'Temp', x: '7', y: 1.62 },
                              { name: 'Temp', x: '8', y: 1.48 },
                              { name: 'Temp', x: '9', y: 2.23 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
              </Grid>
            </AccordionContent>
          </AccordionItem>
          <AccordionItem>
            <AccordionToggle
              id="cpu3-toggle"
              onClick={() => {
                if (openCPU === 'cpu3') {
                  setOpenCPU(null);
                } else {
                  setOpenCPU('cpu3');
                }
              }}
              isExpanded={openCPU === 'cpu3'}
            >
              CPU 3
            </AccordionToggle>
            <AccordionContent isHidden={openCPU !== 'cpu3'}>
              <Grid hasGutter>
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Temperature</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>64C</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">100C</div>
                          <div className="pf-l-flex__item">50C</div>
                          <div className="pf-l-flex__item">0C</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU temperature"
                          ariaTitle="Mock CPU temperature sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 100 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 25 },
                              { name: 'Temp', x: '2', y: 40 },
                              { name: 'Temp', x: '3', y: 20 },
                              { name: 'Temp', x: '4', y: 60 },
                              { name: 'Temp', x: '5', y: 20 },
                              { name: 'Temp', x: '6', y: 41 },
                              { name: 'Temp', x: '7', y: 45 },
                              { name: 'Temp', x: '8', y: 41 },
                              { name: 'Temp', x: '9', y: 62 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
                <Divider className="pf-u-hidden-on-md" />
                <Grid hasGutter>
                  <GridItem md={4}>
                    <Flex
                      className="pf-u-h-100-on-md"
                      direction={{ md: 'column' }}
                      spaceItems={{ md: 'spaceItemsNone' }}
                      justifyContent={{ md: 'justifyContentCenter' }}
                    >
                      <FlexItem>
                        <b>Speed</b>
                      </FlexItem>
                      <Divider
                        className="pf-u-hidden-on-md"
                        orientation={{
                          default: 'vertical'
                        }}
                        inset={{ default: 'insetSm' }}
                      />
                      <FlexItem>
                        <span>2.3Ghz</span>
                      </FlexItem>
                    </Flex>
                  </GridItem>
                  <GridItem md={8}>
                    <Grid hasGutter>
                      <GridItem span={2}>
                        <Flex
                          direction={{ default: 'column' }}
                          spaceItems={{ default: 'spaceItemsNone' }}
                          alignItems={{ md: 'alignItemsFlexEnd' }}
                        >
                          <div className="pf-l-flex__item">3.6Ghz</div>
                          <div className="pf-l-flex__item">1.5Ghz</div>
                          <div className="pf-l-flex__item">0GHZ</div>
                        </Flex>
                      </GridItem>
                      <GridItem span={10}>
                        <ChartGroup
                          ariaDesc="Mock CPU speed"
                          ariaTitle="Mock CPU speed sparkline chart"
                          containerComponent={
                            <ChartVoronoiContainer
                              labels={({ datum }) => `${datum.name}: ${datum.y}`}
                              constrainToVisibleArea
                            />
                          }
                          height={60}
                          maxDomain={{ y: 3.6 }}
                          padding={0}
                          width={400}
                        >
                          <ChartArea
                            data={[
                              { name: 'Temp', x: '1', y: 0.9 },
                              { name: 'Temp', x: '2', y: 1.44 },
                              { name: 'Temp', x: '3', y: 0.72 },
                              { name: 'Temp', x: '4', y: 2.16 },
                              { name: 'Temp', x: '5', y: 0.72 },
                              { name: 'Temp', x: '6', y: 1.48 },
                              { name: 'Temp', x: '7', y: 1.62 },
                              { name: 'Temp', x: '8', y: 1.48 },
                              { name: 'Temp', x: '9', y: 2.23 }
                            ]}
                          />
                        </ChartGroup>
                      </GridItem>
                    </Grid>
                  </GridItem>
                </Grid>
              </Grid>
            </AccordionContent>
          </AccordionItem>
        </Accordion>
      </CardBody>
    </Card>
  );
};
```

### Trend card 1

```ts
import React from 'react';
import {
  Card,
  CardTitle,
  CardBody,
  CardHeader,
  CardActions,
  Title,
  Gallery,
  GalleryItem,
  Flex,
  FlexItem,
  Divider,
  Select,
  SelectOption
} from '@patternfly/react-core';
import { ChartArea, ChartContainer, ChartGroup, ChartLabel, ChartVoronoiContainer } from '@patternfly/react-charts';

const TrendCard1: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const selectItems = [
    <SelectOption value="Last hour" key="option1" />,
    <SelectOption value="Last 6 hours" key="option2" />,
    <SelectOption value="Last 24 hours" key="option3" />,
    <SelectOption value="Last 7 days" key="option4" />
  ];
  return (
    <React.Fragment>
      <b>Note:</b> Custom CSS is used in this demo to align the card title and select toggle text to{' '}
      <code>baseline</code> alignment.
      <br />
      <br />
      <Gallery hasGutter minWidths={{ default: '360px' }}>
        <GalleryItem>
          <Card id="trend-card-1" component="div">
            <CardHeader>
              <Flex direction={{ default: 'column' }} spaceItems={{ default: 'spaceItemsNone' }}>
                <FlexItem>
                  <CardTitle>
                    <Title headingLevel="h1">1,050,765 IOPS</Title>
                  </CardTitle>
                </FlexItem>
                <FlexItem>
                  <span className="pf-u-color-200">Workload</span>
                </FlexItem>
              </Flex>
              <CardActions hasNoOffset style={{ paddingTop: '1px' }}>
                <Select
                  onSelect={() => setIsOpen(!isOpen)}
                  onToggle={() => setIsOpen(!isOpen)}
                  placeholderText="Filter"
                  isOpen={isOpen}
                  position="right"
                  isPlain
                >
                  {selectItems}
                </Select>
              </CardActions>
            </CardHeader>
            <CardBody>
              <ChartGroup
                ariaDesc="Mock average cluster utilization"
                ariaTitle="Mock cluster sparkline chart"
                containerComponent={
                  <ChartVoronoiContainer labels={({ datum }) => `${datum.name}: ${datum.y}`} constrainToVisibleArea />
                }
                height={100}
                maxDomain={{ y: 9 }}
                padding={0}
                width={400}
              >
                <ChartArea
                  data={[
                    { name: 'Cluster', x: '2015', y: 7 },
                    { name: 'Cluster', x: '2016', y: 6 },
                    { name: 'Cluster', x: '2017', y: 8 },
                    { name: 'Cluster', x: '2018', y: 3 },
                    { name: 'Cluster', x: '2019', y: 4 },
                    { name: 'Cluster', x: '2020', y: 1 },
                    { name: 'Cluster', x: '2021', y: 0 }
                  ]}
                />
              </ChartGroup>
            </CardBody>
          </Card>
        </GalleryItem>
      </Gallery>
    </React.Fragment>
  );
};
```

### Trend card 2

```ts
import React from 'react';
import {
  Card,
  CardTitle,
  CardFooter,
  CardHeader,
  Title,
  Gallery,
  GalleryItem,
  Flex,
  FlexItem
} from '@patternfly/react-core';
import { ChartArea, ChartContainer, ChartGroup, ChartLabel, ChartVoronoiContainer } from '@patternfly/react-charts';

<Gallery hasGutter minWidths={{ default: '360px' }}>
  <GalleryItem>
    <Card id="trend-card-2" component="div">
      <CardHeader>
        <Flex alignItems={{ default: 'alignItemsCenter' }}>
          <FlexItem flex={{ default: 'flexNone' }}>
            <Flex direction={{ default: 'column' }} spaceItems={{ default: 'spaceItemsNone' }}>
              <FlexItem>
                <CardTitle>
                  <Title headingLevel="h1" size="2xl">
                    842 TB
                  </Title>
                </CardTitle>
              </FlexItem>
              <FlexItem>
                <span className="pf-u-color-200">Storage capacity</span>
              </FlexItem>
            </Flex>
          </FlexItem>
          <FlexItem flex={{ default: 'flex_1' }}>
            <ChartGroup
              ariaDesc="Mock average cluster utilization"
              ariaTitle="Mock cluster sparkline chart"
              containerComponent={
                <ChartVoronoiContainer labels={({ datum }) => `${datum.name}: ${datum.y}`} constrainToVisibleArea />
              }
              height={100}
              maxDomain={{ y: 9 }}
              padding={0}
              width={400}
            >
              <ChartArea
                data={[
                  { name: 'Cluster', x: '2015', y: 7 },
                  { name: 'Cluster', x: '2016', y: 6 },
                  { name: 'Cluster', x: '2017', y: 8 },
                  { name: 'Cluster', x: '2018', y: 3 },
                  { name: 'Cluster', x: '2019', y: 4 },
                  { name: 'Cluster', x: '2020', y: 1 },
                  { name: 'Cluster', x: '2021', y: 0 }
                ]}
              />
            </ChartGroup>
          </FlexItem>
        </Flex>
      </CardHeader>
      <CardFooter>
        <Flex>
          <FlexItem>
            <a href="#">Action 1</a>
          </FlexItem>
          <FlexItem>
            <a href="#">Action 2</a>
          </FlexItem>
        </Flex>
      </CardFooter>
    </Card>
  </GalleryItem>
</Gallery>;
```

### Log view

```js
import React from 'react';
import {
  Card,
  CardHeader,
  CardActions,
  CardTitle,
  CardBody,
  CardFooter,
  Gallery,
  Title,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Select,
  SelectOption,
  Divider
} from '@patternfly/react-core';

CardLogViewDemo = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onActionSelect = event => {
    setIsOpen(!isOpen);
  };

  const onActionToggle = isOpen => {
    setIsOpen(isOpen);
  };

  const selectItems = [
    <SelectOption value="Most recent" key="option1" />,
    <SelectOption value="Last 6 hours" key="option2" />,
    <SelectOption value="Last 24 hours" key="option3" />,
    <SelectOption value="Last 7 days" key="option4" />
  ];

  return (
    <React.Fragment>
      <b>Note:</b> Custom CSS is used in this demo to align the card title and select toggle text to{' '}
      <code>baseline</code> alignment.
      <br />
      <br />
      <Gallery hasGutter style={{ '--pf-l-gallery--GridTemplateColumns--min': '360px' }}>
        <Card id="card-log-view-example">
          <CardHeader className="pf-u-align-items-flex-start">
            <CardActions hasNoOffset>
              <Select
                onSelect={onActionSelect}
                onToggle={onActionToggle}
                placeholderText="Most recent"
                isOpen={isOpen}
                position="right"
                isPlain
              >
                {selectItems}
              </Select>
            </CardActions>
            <CardTitle>
              <Title headingLevel="h2" size="xl" style={{ paddingTop: '3px' }}>
                Activity
              </Title>
            </CardTitle>
          </CardHeader>
          <CardBody>
            <DescriptionList>
              <DescriptionListGroup>
                <DescriptionListTerm>Readiness probe failed</DescriptionListTerm>
                <DescriptionListDescription>
                  Readiness probe failed: Get https://10.131.0.7:5000/healthz: dial tcp 10.131.0.7:5000: connect:
                  connection refused
                </DescriptionListDescription>
                <DescriptionListDescription>
                  <time className="pf-u-color-200 pf-u-font-size-sm">Jun 17, 11:02 am</time>
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>Successful assignment</DescriptionListTerm>
                <DescriptionListDescription>
                  Successfully assigned default/example to ip-10-0-130-149.ec2.internal
                </DescriptionListDescription>
                <DescriptionListDescription>
                  <time className="pf-u-color-200 pf-u-font-size-sm">Jun 17, 11:13 am</time>
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>Pulling image</DescriptionListTerm>
                <DescriptionListDescription>Pulling image "openshift/hello-openshift"</DescriptionListDescription>
                <DescriptionListDescription>
                  <time className="pf-u-color-200 pf-u-font-size-sm">Jun 17, 10:59 am</time>
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>Created container</DescriptionListTerm>
                <DescriptionListDescription>Created container hello-openshift</DescriptionListDescription>
                <DescriptionListDescription>
                  <time className="pf-u-color-200 pf-u-font-size-sm">Jun 17, 10:45 am</time>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </DescriptionList>
          </CardBody>
          <Divider />
          <CardFooter>
            <a href="#">View all activity</a>
          </CardFooter>
        </Card>
      </Gallery>
    </React.Fragment>
  );
};
```

### Events view

```js
import React from 'react';
import {
  Card,
  CardHeader,
  CardActions,
  CardTitle,
  CardBody,
  CardFooter,
  Gallery,
  Flex,
  FlexItem,
  Title,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Spinner,
  Select,
  SelectOption,
  Divider
} from '@patternfly/react-core';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';

CardEventViewDemo = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onActionSelect = event => {
    setIsOpen(!isOpen);
  };

  const onActionToggle = isOpen => {
    setIsOpen(isOpen);
  };

  const selectItems = [
    <SelectOption value="Success" key="option1" />,
    <SelectOption value="Error" key="option2" />,
    <SelectOption value="Warning" key="option3" />
  ];

  return (
    <React.Fragment>
      <b>Note:</b> Custom CSS is used in this demo to align the card title and select toggle text to{' '}
      <code>baseline</code> alignment.
      <br />
      <br />
      <Gallery hasGutter style={{ '--pf-l-gallery--GridTemplateColumns--min': '360px' }}>
        <Card id="card-events-view-example">
          <CardHeader className="pf-u-align-items-flex-start">
            <CardActions hasNoOffset>
              <Select
                onSelect={onActionSelect}
                onToggle={onActionToggle}
                placeholderText="Status"
                isOpen={isOpen}
                position="right"
                isPlain
              >
                {selectItems}
              </Select>
            </CardActions>
            <CardTitle>
              <Title headingLevel="h2" size="xl" style={{ paddingTop: '3px' }}>
                Events
              </Title>
            </CardTitle>
          </CardHeader>
          <CardBody>
            <DescriptionList>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  <Flex flexWrap={{ default: 'nowrap' }}>
                    <FlexItem>
                      <ExclamationCircleIcon className="pf-u-danger-color-100" aria-hidden="true" />
                    </FlexItem>
                    <FlexItem>
                      <span>Readiness probe failed</span>
                    </FlexItem>
                  </Flex>
                </DescriptionListTerm>
                <DescriptionListDescription>
                  Readiness probe failed: Get https://10.131.0.7:5000/healthz: dial tcp 10.131.0.7:5000: connect:
                  connection refused
                </DescriptionListDescription>
                <DescriptionListDescription>
                  <time className="pf-u-color-200 pf-u-font-size-sm">Jun 17, 11:02 am</time>
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  <Flex flexWrap={{ default: 'nowrap' }}>
                    <FlexItem>
                      <CheckCircleIcon className="pf-u-success-color-100" aria-hidden="true" />
                    </FlexItem>
                    <FlexItem>
                      <span>Successful assignment</span>
                    </FlexItem>
                  </Flex>
                </DescriptionListTerm>
                <DescriptionListDescription>
                  Successfully assigned default/example to ip-10-0-130-149.ec2.internal
                </DescriptionListDescription>
                <DescriptionListDescription>
                  <time className="pf-u-color-200 pf-u-font-size-sm">Jun 17, 11:13 am</time>
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  <Flex flexWrap={{ default: 'nowrap' }}>
                    <FlexItem>
                      <Spinner size="md" aria-label="loading spinner" />
                    </FlexItem>
                    <FlexItem>
                      <span>Pulling image</span>
                    </FlexItem>
                  </Flex>
                </DescriptionListTerm>
                <DescriptionListDescription>Pulling image "openshift/hello-openshift"</DescriptionListDescription>
                <DescriptionListDescription>
                  <time className="pf-u-color-200 pf-u-font-size-sm">Jun 17, 10:59 am</time>
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  <Flex flexWrap={{ default: 'nowrap' }}>
                    <FlexItem>
                      <CheckCircleIcon className="pf-u-success-color-100" aria-hidden="true" />
                    </FlexItem>
                    <FlexItem>
                      <span>Created container</span>
                    </FlexItem>
                  </Flex>
                </DescriptionListTerm>
                <DescriptionListDescription>Created container hello-openshift</DescriptionListDescription>
                <DescriptionListDescription>
                  <time className="pf-u-color-200 pf-u-font-size-sm">Jun 17, 10:45 am</time>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </DescriptionList>
          </CardBody>
          <Divider />
          <CardFooter>
            <a href="#">View all events</a>
          </CardFooter>
        </Card>
      </Gallery>
    </React.Fragment>
  );
};
```
