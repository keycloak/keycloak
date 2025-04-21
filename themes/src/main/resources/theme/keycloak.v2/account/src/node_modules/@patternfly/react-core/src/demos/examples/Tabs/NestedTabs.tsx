import React from 'react';
import {
  Card,
  CardHeader,
  CardBody,
  Grid,
  GridItem,
  PageSection,
  Tabs,
  Tab,
  TabContent,
  TabContentBody,
  TabTitleText,
  Title,
  Flex,
  FlexItem
} from '@patternfly/react-core';
import DashboardWrapper from '../DashboardWrapper';

export const NestedTabs: React.FunctionComponent = () => {
  const [activeTabKey, setActiveTabKey] = React.useState(0);
  const [activeNestedTabKey, setActiveNestedTabKey] = React.useState(10);

  // Toggle currently active tab
  const handleTabClick = (tabIndex: number) => setActiveTabKey(tabIndex);

  // Toggle currently active nested tab
  const handleNestedTabClick = (tabIndex: number) => setActiveNestedTabKey(tabIndex);

  const tabContent = (
    <Grid hasGutter>
      <GridItem xl={8} md={6}>
        <Card>
          <CardHeader>
            <Title headingLevel="h2">Status</Title>
          </CardHeader>
          <CardBody>
            <Flex direction={{ default: 'column' }}>
              <FlexItem>
                <Tabs
                  activeKey={activeNestedTabKey}
                  isSecondary
                  onSelect={(_event, tabIndex) => handleNestedTabClick(Number(tabIndex))}
                  id="nested-tabs-example-nested-tabs-list"
                >
                  <Tab eventKey={10} title={<TabTitleText>Cluster</TabTitleText>} tabContentId={`tabContent${10}`} />
                  <Tab
                    eventKey={11}
                    title={<TabTitleText>Control plane</TabTitleText>}
                    tabContentId={`tabContent${11}`}
                  />
                  <Tab eventKey={12} title={<TabTitleText>Operators</TabTitleText>} tabContentId={`tabContent${12}`} />
                  <Tab
                    eventKey={13}
                    title={<TabTitleText>Virtualization</TabTitleText>}
                    tabContentId={`tabContent${13}`}
                  />
                </Tabs>
              </FlexItem>
              <FlexItem>
                <TabContent
                  key={10}
                  eventKey={10}
                  id={`tabContent${10}`}
                  activeKey={activeNestedTabKey}
                  hidden={10 !== activeNestedTabKey}
                >
                  <TabContentBody>
                    {
                      'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce in odio porttitor, feugiat risus in, feugiat arcu. Nullam euismod enim eget fringilla condimentum. Maecenas tincidunt et metus id aliquet. Integer et fermentum purus. Nulla tempor velit arcu, vitae semper purus iaculis at. Sed malesuada auctor luctus. Pellentesque et leo urna. Aliquam vitae felis congue lacus mattis fringilla. Nullam et ultricies erat, sed dignissim elit. Cras mattis pulvinar aliquam. In ac est nulla. Pellentesque fermentum nibh ac sapien porta, ut congue orci aliquam. Sed nisl est, tempor eu pharetra eget, ullamcorper ut augue. Vestibulum eleifend libero eu nulla cursus lacinia.'
                    }
                  </TabContentBody>
                </TabContent>
                <TabContent
                  key={11}
                  eventKey={11}
                  id={`tabContent${11}`}
                  activeKey={activeNestedTabKey}
                  hidden={11 !== activeNestedTabKey}
                >
                  <TabContentBody>Control plane panel</TabContentBody>
                </TabContent>
                <TabContent
                  key={12}
                  eventKey={12}
                  id={`tabContent${12}`}
                  activeKey={activeNestedTabKey}
                  hidden={12 !== activeNestedTabKey}
                >
                  <TabContentBody>Operators panel</TabContentBody>
                </TabContent>
                <TabContent
                  key={13}
                  eventKey={13}
                  id={`tabContent${13}`}
                  activeKey={activeNestedTabKey}
                  hidden={13 !== activeNestedTabKey}
                >
                  <TabContentBody>Virtualization panel</TabContentBody>
                </TabContent>
              </FlexItem>
            </Flex>
          </CardBody>
        </Card>
      </GridItem>
      <GridItem xl={4} md={6}>
        <Flex direction={{ default: 'column' }} className="pf-u-h-100">
          <FlexItem flex={{ default: 'flex_1' }}>
            <Card isFullHeight>
              <CardHeader>
                <Title headingLevel="h2">Title of Card</Title>
              </CardHeader>
            </Card>
          </FlexItem>
          <FlexItem flex={{ default: 'flex_1' }}>
            <Card isFullHeight>
              <CardHeader>
                <Title headingLevel="h2">Title of Card</Title>
              </CardHeader>
            </Card>
          </FlexItem>
        </Flex>
      </GridItem>
    </Grid>
  );

  return (
    <DashboardWrapper hasPageTemplateTitle>
      <PageSection type="tabs" isWidthLimited>
        <Tabs
          activeKey={activeTabKey}
          onSelect={(_event, tabIndex) => handleTabClick(Number(tabIndex))}
          usePageInsets
          id="nested-tabs-example-tabs-list"
        >
          <Tab eventKey={0} title={<TabTitleText>Cluster 1</TabTitleText>} tabContentId={`tabContent${0}`} />
          <Tab eventKey={1} title={<TabTitleText>Cluster 2</TabTitleText>} tabContentId={`tabContent${1}`} />
        </Tabs>
      </PageSection>
      <PageSection isWidthLimited>
        <TabContent key={0} eventKey={0} id={`tabContent${0}`} activeKey={activeTabKey} hidden={0 !== activeTabKey}>
          <TabContentBody>{tabContent}</TabContentBody>
        </TabContent>
        <TabContent key={1} eventKey={1} id={`tabContent${1}`} activeKey={activeTabKey} hidden={1 !== activeTabKey}>
          <TabContentBody>Cluster 2 panel</TabContentBody>
        </TabContent>
      </PageSection>
    </DashboardWrapper>
  );
};
