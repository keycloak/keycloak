import React from 'react';
import DashboardWrapper from '../DashboardWrapper';
import {
  PageSection,
  TextContent,
  Text,
  Gallery,
  Card,
  CardBody,
  CardTitle,
  PageSectionVariants,
  Modal,
  ModalVariant,
  Tab,
  Tabs,
  TabTitleText,
  List,
  ListItem,
  Grid,
  GridItem,
  TabContent
} from '@patternfly/react-core';

interface Product {
  id: number;
  name: string;
  description: string;
}

const products: Product[] = [
  {
    id: 0,
    name: 'PatternFly',
    description: 'PatternFly is a community project that promotes design commonality and improves user experience.'
  },
  {
    id: 1,
    name: 'ActiveMQ',
    description:
      'The ActiveMQ component allows messages to be sent to a JMS Queue or Topic; or messages to be consumed from a JMS Queue or Topic using Apache ActiveMQ.'
  },
  {
    id: 2,
    name: 'Apache Spark',
    description: 'This documentation page covers the Apache Spark component for the Apache Camel.'
  }
];

export const ModalTabs: React.FunctionComponent = () => {
  const [isModalOpen, setIsModalOpen] = React.useState(false);
  const [selectedProduct, setSelectedProduct] = React.useState<Product>();
  const [activeTabKey, setActiveTabKey] = React.useState<string | number>(0);

  const onCardClick = React.useCallback(
    (product: Product) => () => {
      setSelectedProduct(product);
      setIsModalOpen(true);
    },
    []
  );

  const onCardKeyPress = React.useCallback(
    (product: Product) => (event: React.KeyboardEvent<HTMLElement>) => {
      if (event.key === 'Enter' || event.key === ' ') {
        onCardClick(product)();
      }
    },
    []
  );

  const closeModal = React.useCallback(() => {
    setSelectedProduct(undefined);
    setIsModalOpen(false);
    setActiveTabKey(0);
  }, []);

  const onTabSelect = React.useCallback(
    (_event: React.MouseEvent<HTMLElement, MouseEvent>, tabIndex: string | number) => setActiveTabKey(tabIndex),
    []
  );

  return (
    <React.Fragment>
      <DashboardWrapper mainContainerId="main-content-card-view-default-nav">
        <PageSection variant={PageSectionVariants.light}>
          <TextContent>
            <Text component="h1">Projects</Text>
            <Text component="p">Click any project card to view Tabs within Modals.</Text>
          </TextContent>
        </PageSection>
        <PageSection isFilled>
          <Gallery hasGutter aria-label="Selectable card container">
            {products.map(product => (
              <Card
                isSelectable
                isSelectableRaised
                hasSelectableInput
                isCompact
                key={product.id}
                id={product.name.replace(/ /g, '-')}
                onClick={onCardClick(product)}
                onSelectableInputChange={() => onCardClick(product)()}
                onKeyPress={onCardKeyPress(product)}
              >
                <CardTitle>{product.name}</CardTitle>
                <CardBody>{product.description}</CardBody>
              </Card>
            ))}
          </Gallery>
        </PageSection>
      </DashboardWrapper>

      {selectedProduct && (
        <Modal variant={ModalVariant.small} title={selectedProduct.name} isOpen={isModalOpen} onClose={closeModal}>
          <Grid hasGutter>
            <GridItem>
              <Tabs activeKey={activeTabKey} onSelect={onTabSelect} isSecondary>
                <Tab eventKey={0} tabContentId="details-tab" title={<TabTitleText>Details</TabTitleText>} />
                <Tab eventKey={1} tabContentId="doc-tab" title={<TabTitleText>Documentation</TabTitleText>} />
              </Tabs>
            </GridItem>
            <GridItem>
              <TabContent eventKey={0} id="details-tab" hidden={activeTabKey !== 0}>
                {selectedProduct.description}
              </TabContent>
              <TabContent eventKey={1} id="doc-tab" hidden={activeTabKey !== 1}>
                <List>
                  <ListItem>
                    <a>Doc link 1</a>
                  </ListItem>
                  <ListItem>
                    <a>Doc link 2</a>
                  </ListItem>
                  <ListItem>
                    <a>Doc link 3</a>
                  </ListItem>
                </List>
              </TabContent>
            </GridItem>
          </Grid>
        </Modal>
      )}
    </React.Fragment>
  );
};
