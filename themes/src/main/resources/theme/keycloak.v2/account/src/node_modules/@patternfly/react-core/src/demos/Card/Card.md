---
id: Card view
section: demos
---

import DashboardWrapper from '../examples/DashboardWrapper';

import FilterIcon from '@patternfly/react-icons/dist/esm/icons/filter-icon';
import TrashIcon from '@patternfly/react-icons/dist/esm/icons/trash-icon';
import PlusCircleIcon from '@patternfly/react-icons/dist/esm/icons/plus-circle-icon';
import pfIcon from './pf-logo-small.svg';
import activeMQIcon from './activemq-core_200x150.png';
import avroIcon from './camel-avro_200x150.png';
import dropBoxIcon from './camel-dropbox_200x150.png';
import infinispanIcon from './camel-infinispan_200x150.png';
import saxonIcon from './camel-saxon_200x150.png';
import sparkIcon from './camel-spark_200x150.png';
import swaggerIcon from './camel-swagger-java_200x150.png';
import azureIcon from './FuseConnector_Icons_AzureServices.png';
import restIcon from './FuseConnector_Icons_REST.png';

## Demos

This demonstrates how you can assemble a full page view that contains a grid of equal sized cards that includes a toolbar for managing card grid contents.

### Card view

```js isFullscreen
import React from 'react';
import {
  Bullseye,
  Button,
  Card,
  CardHeader,
  CardActions,
  CardTitle,
  CardBody,
  Checkbox,
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  DropdownPosition,
  DropdownToggleCheckbox,
  EmptyState,
  EmptyStateIcon,
  EmptyStateVariant,
  EmptyStateSecondaryActions,
  Gallery,
  KebabToggle,
  OverflowMenu,
  OverflowMenuControl,
  OverflowMenuDropdownItem,
  OverflowMenuItem,
  PageSection,
  PageSectionVariants,
  Pagination,
  Select,
  SelectOption,
  SelectVariant,
  TextContent,
  Text,
  Title,
  Toolbar,
  ToolbarItem,
  ToolbarFilter,
  ToolbarContent
} from '@patternfly/react-core';
import DashboardWrapper from '../examples/DashboardWrapper';

import FilterIcon from '@patternfly/react-icons/dist/esm/icons/filter-icon';
import TrashIcon from '@patternfly/react-icons/dist/esm/icons/trash-icon';
import PlusCircleIcon from '@patternfly/react-icons/dist/esm/icons/plus-circle-icon';
import pfIcon from './pf-logo-small.svg';
import activeMQIcon from './activemq-core_200x150.png';
import avroIcon from './camel-avro_200x150.png';
import dropBoxIcon from './camel-dropbox_200x150.png';
import infinispanIcon from './camel-infinispan_200x150.png';
import saxonIcon from './camel-saxon_200x150.png';
import sparkIcon from './camel-spark_200x150.png';
import swaggerIcon from './camel-swagger-java_200x150.png';
import azureIcon from './FuseConnector_Icons_AzureServices.png';
import restIcon from './FuseConnector_Icons_REST.png';

class CardViewBasic extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      filters: {
        products: []
      },
      res: [],
      isChecked: false,
      selectedItems: [],
      areAllSelected: false,
      isUpperToolbarDropdownOpen: false,
      isUpperToolbarKebabDropdownOpen: false,
      isLowerToolbarDropdownOpen: false,
      isLowerToolbarKebabDropdownOpen: false,
      isCardKebabDropdownOpen: false,
      activeItem: 0,
      splitButtonDropdownIsOpen: false,
      page: 1,
      perPage: 10,
      totalItemCount: 10
    };

    this.checkAllSelected = (selected, total) => {
      if (selected && selected < total) {
        return null;
      }
      return selected === total;
    };

    this.onToolbarDropdownToggle = isLowerToolbarDropdownOpen => {
      this.setState(prevState => ({
        isLowerToolbarDropdownOpen
      }));
    };

    this.onToolbarKebabDropdownToggle = isLowerToolbarKebabDropdownOpen => {
      this.setState({
        isLowerToolbarKebabDropdownOpen
      });
    };

    this.onToolbarKebabDropdownSelect = event => {
      this.setState({
        isLowerToolbarKebabDropdownOpen: !this.state.isLowerToolbarKebabDropdownOpen
      });
    };

    this.onCardKebabDropdownToggle = (key, isCardKebabDropdownOpen) => {
      this.setState({
        [key]: isCardKebabDropdownOpen
      });
    };

    this.onCardKebabDropdownSelect = (key, event) => {
      this.setState({
        [key]: !this.state[key]
      });
    };

    this.deleteItem = item => event => {
      const filter = getter => val => getter(val) !== item.id;
      this.setState({
        res: this.state.res.filter(filter(({ id }) => id)),
        selectedItems: this.state.selectedItems.filter(filter(id => id))
      });
    };

    this.onSetPage = (_event, pageNumber) => {
      this.setState({
        page: pageNumber
      });
    };

    this.onPerPageSelect = (_event, perPage) => {
      this.setState({
        perPage
      });
    };

    this.onSplitButtonToggle = isOpen => {
      this.setState({
        splitButtonDropdownIsOpen: isOpen
      });
    };

    this.onSplitButtonSelect = event => {
      this.setState((prevState, props) => {
        return { splitButtonDropdownIsOpen: !prevState.splitButtonDropdownIsOpen };
      });
    };

    this.onNameSelect = (event, selection) => {
      const checked = event.target.checked;
      this.setState(prevState => {
        const prevSelections = prevState.filters['products'];
        return {
          filters: {
            ...prevState.filters,
            ['products']: checked ? [...prevSelections, selection] : prevSelections.filter(value => value !== selection)
          }
        };
      });
    };

    this.onDelete = (type = '', id = '') => {
      if (type) {
        this.setState(prevState => {
          prevState.filters[type.toLowerCase()] = prevState.filters[type.toLowerCase()].filter(s => s !== id);
          return {
            filters: prevState.filters
          };
        });
      } else {
        this.setState({
          filters: {
            products: []
          }
        });
      }
    };

    this.onKeyDown = (event, productId) => {
      console.log(productId);
      if (event.target !== event.currentTarget) {
        return;
      }
      if ([' ', 'Enter'].includes(event.key)) {
        event.preventDefault();
        this.setState(prevState => {
          return prevState.selectedItems.includes(productId * 1)
            ? {
                selectedItems: [...prevState.selectedItems.filter(id => productId * 1 != id)],
                areAllSelected: this.checkAllSelected(prevState.selectedItems.length - 1, prevState.totalItemCount)
              }
            : {
                selectedItems: [...prevState.selectedItems, productId * 1],
                areAllSelected: this.checkAllSelected(prevState.selectedItems.length + 1, prevState.totalItemCount)
              };
        });
      }
    };

    this.onClick = productId => {
      this.setState(prevState => {
        return prevState.selectedItems.includes(productId * 1)
          ? {
              selectedItems: [...prevState.selectedItems.filter(id => productId * 1 != id)],
              areAllSelected: this.checkAllSelected(prevState.selectedItems.length - 1, prevState.totalItemCount)
            }
          : {
              selectedItems: [...prevState.selectedItems, productId * 1],
              areAllSelected: this.checkAllSelected(prevState.selectedItems.length + 1, prevState.totalItemCount)
            };
      });
    };
  }

  selectedItems(e) {
    const { value, checked } = e.target;
    let { selectedItems } = this.state;

    if (checked) {
      selectedItems = [...selectedItems, value];
    } else {
      selectedItems = selectedItems.filter(el => el !== value);
      if (this.state.areAllSelected) {
        this.setState({
          areAllSelected: !this.state.areAllSelected
        });
      }
    }
    this.setState({ selectedItems });
  }

  splitCheckboxSelectAll(e) {
    const { checked } = e.target;
    const { isChecked, res } = this.state;
    let collection = [];

    if (checked) {
      for (var i = 0; i <= 9; i++) collection = [...collection, i];
    }

    this.setState(
      {
        selectedItems: collection,
        isChecked: isChecked,
        areAllSelected: checked
      },
      this.updateSelected
    );
  }

  selectPage(e) {
    const { checked } = e.target;
    const { isChecked, totalItemCount, perPage } = this.state;
    let collection = [];

    collection = this.getAllItems();

    this.setState(
      {
        selectedItems: collection,
        isChecked: checked,
        areAllSelected: totalItemCount === perPage ? true : false
      },
      this.updateSelected
    );
  }

  selectAll(e) {
    const { checked } = e.target;
    const { isChecked } = this.state;

    let collection = [];
    for (var i = 0; i <= 9; i++) collection = [...collection, i];

    this.setState(
      {
        selectedItems: collection,
        isChecked: true,
        areAllSelected: true
      },
      this.updateSelected
    );
  }

  selectNone(e) {
    const { checked } = e.target;
    const { isChecked, selectedItems } = this.state;
    this.setState(
      {
        selectedItems: [],
        isChecked: false,
        areAllSelected: false
      },
      this.updateSelected
    );
  }

  getAllItems() {
    const { res } = this.state;
    const collection = [];
    for (const items of res) {
      collection.push(items.id);
    }

    return collection;
  }

  updateSelected() {
    const { res, selectedItems } = this.state;
    let rows = res.map(post => {
      post.selected = selectedItems.includes(post.id);
      return post;
    });

    this.setState({
      res: rows
    });
  }

  fetch(page, perPage) {
    fetch(`https://my-json-server.typicode.com/jenny-s51/cardviewdata/posts?_page=${page}&_limit=${perPage}`)
      .then(resp => resp.json())
      .then(resp => this.setState({ res: resp, perPage, page }))
      .then(() => this.updateSelected())
      .catch(err => this.setState({ error: err }));
  }

  componentDidMount() {
    this.fetch(this.state.page, this.state.perPage);
  }

  renderPagination() {
    const { page, perPage, totalItemCount } = this.state;

    const defaultPerPageOptions = [
      {
        title: '1',
        value: 1
      },
      {
        title: '5',
        value: 5
      },
      {
        title: '10',
        value: 10
      }
    ];

    return (
      <Pagination
        itemCount={totalItemCount}
        page={page}
        perPage={perPage}
        perPageOptions={defaultPerPageOptions}
        onSetPage={(_evt, value) => {
          this.fetch(value, perPage);
        }}
        onPerPageSelect={(_evt, value) => {
          this.fetch(1, value);
        }}
        variant="top"
        isCompact
      />
    );
  }

  buildSelectDropdown() {
    const { splitButtonDropdownIsOpen, selectedItems, areAllSelected } = this.state;
    const numSelected = selectedItems.length;
    const allSelected = areAllSelected;
    const anySelected = numSelected > 0;
    const someChecked = anySelected ? null : false;
    const isChecked = allSelected ? true : someChecked;
    const splitButtonDropdownItems = [
      <DropdownItem key="item-1" onClick={this.selectNone.bind(this)}>
        Select none (0 items)
      </DropdownItem>,
      <DropdownItem key="item-2" onClick={this.selectPage.bind(this)}>
        Select page ({this.state.perPage} items)
      </DropdownItem>,
      <DropdownItem key="item-3" onClick={this.selectAll.bind(this)}>
        Select all ({this.state.totalItemCount} items)
      </DropdownItem>
    ];

    return (
      <Dropdown
        position={DropdownPosition.left}
        onSelect={this.onSplitButtonSelect}
        toggle={
          <DropdownToggle
            splitButtonItems={[
              <DropdownToggleCheckbox
                id="example-checkbox-2"
                key="split-checkbox"
                aria-label={anySelected ? 'Deselect all' : 'Select all'}
                isChecked={areAllSelected}
                onClick={this.splitCheckboxSelectAll.bind(this)}
              ></DropdownToggleCheckbox>
            ]}
            onToggle={this.onSplitButtonToggle}
          >
            {numSelected !== 0 && <React.Fragment>{numSelected} selected</React.Fragment>}
          </DropdownToggle>
        }
        isOpen={splitButtonDropdownIsOpen}
        dropdownItems={splitButtonDropdownItems}
      />
    );
  }

  buildFilterDropdown() {
    const { isLowerToolbarDropdownOpen, filters } = this.state;

    const filterDropdownItems = [
      <SelectOption key="patternfly" value="PatternFly" />,
      <SelectOption key="activemq" value="ActiveMQ" />,
      <SelectOption key="apachespark" value="Apache Spark" />,
      <SelectOption key="avro" value="Avro" />,
      <SelectOption key="azureservices" value="Azure Services" />,
      <SelectOption key="crypto" value="Crypto" />,
      <SelectOption key="dropbox" value="DropBox" />,
      <SelectOption key="jbossdatagrid" value="JBoss Data Grid" />,
      <SelectOption key="rest" value="REST" />,
      <SelectOption key="swagger" value="SWAGGER" />
    ];

    return (
      <ToolbarFilter categoryName="Products" chips={filters.products} deleteChip={this.onDelete}>
        <Select
          variant={SelectVariant.checkbox}
          aria-label="Products"
          onToggle={this.onToolbarDropdownToggle}
          onSelect={this.onNameSelect}
          selections={filters.products}
          isExpanded={isLowerToolbarDropdownOpen}
          placeholderText="Creator"
        >
          {filterDropdownItems}
        </Select>
      </ToolbarFilter>
    );
  }

  render() {
    const {
      isUpperToolbarDropdownOpen,
      isLowerToolbarDropdownOpen,
      isUpperToolbarKebabDropdownOpen,
      isLowerToolbarKebabDropdownOpen,
      isCardKebabDropdownOpen,
      splitButtonDropdownIsOpen,
      activeItem,
      filters,
      res,
      checked,
      selectedItems,
      areAllSelected,
      isChecked,
      page,
      perPage
    } = this.state;

    const toolbarKebabDropdownItems = [
      <OverflowMenuDropdownItem key="link">Link</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="action" component="button">
        Action
      </OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="disabled link" isDisabled>
        Disabled Link
      </OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="disabled action" isDisabled component="button">
        Disabled Action
      </OverflowMenuDropdownItem>,
      <DropdownSeparator key="separator" />,
      <OverflowMenuDropdownItem key="separated link">Separated Link</OverflowMenuDropdownItem>,
      <OverflowMenuDropdownItem key="separated action" component="button">
        Separated Action
      </OverflowMenuDropdownItem>
    ];

    const toolbarItems = (
      <React.Fragment>
        <ToolbarItem variant="bulk-select">{this.buildSelectDropdown()}</ToolbarItem>
        <ToolbarItem breakpoint="xl">{this.buildFilterDropdown()}</ToolbarItem>
        <ToolbarItem variant="overflow-menu">
          <OverflowMenu breakpoint="md">
            <OverflowMenuItem>
              <Button variant="primary">Create a project</Button>
            </OverflowMenuItem>
            <OverflowMenuControl hasAdditionalOptions>
              <Dropdown
                onSelect={this.onToolbarKebabDropdownSelect}
                toggle={<KebabToggle onToggle={this.onToolbarKebabDropdownToggle} id="toggle-id-6" />}
                isOpen={isLowerToolbarKebabDropdownOpen}
                isPlain
                dropdownItems={toolbarKebabDropdownItems}
                isFlipEnabled
                menuAppendTo="parent"
              />
            </OverflowMenuControl>
          </OverflowMenu>
        </ToolbarItem>
        <ToolbarItem variant="pagination" alignment={{ default: 'alignRight' }}>
          {this.renderPagination()}
        </ToolbarItem>
      </React.Fragment>
    );

    const filtered =
      filters.products.length > 0
        ? res.filter(card => {
            return filters.products.length === 0 || filters.products.includes(card.name);
          })
        : res;

    const icons = {
      pfIcon,
      activeMQIcon,
      sparkIcon,
      avroIcon,
      azureIcon,
      saxonIcon,
      dropBoxIcon,
      infinispanIcon,
      restIcon,
      swaggerIcon
    };

    return (
      <React.Fragment>
        <DashboardWrapper mainContainerId="main-content-card-view-default-nav" breadcrumb={null}>
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Projects</Text>
              <Text component="p">This is a demo that showcases PatternFly cards.</Text>
            </TextContent>
            <Toolbar id="toolbar-group-types" clearAllFilters={this.onDelete}>
              <ToolbarContent>{toolbarItems}</ToolbarContent>
            </Toolbar>
          </PageSection>
          <PageSection isFilled>
            <Gallery hasGutter aria-label="Selectable card container">
              <Card isCompact>
                <Bullseye>
                  <EmptyState variant={EmptyStateVariant.xs}>
                    <EmptyStateIcon icon={PlusCircleIcon} />
                    <Title headingLevel="h2" size="md">
                      Add a new card to your page
                    </Title>
                    <EmptyStateSecondaryActions>
                      <Button variant="link">Add card</Button>
                    </EmptyStateSecondaryActions>
                  </EmptyState>
                </Bullseye>
              </Card>
              {filtered.map((product, key) => (
                <Card
                  hasSelectableInput
                  isCompact
                  key={product.name}
                  id={product.name.replace(/ /g, '-')}
                  onKeyDown={e => this.onKeyDown(e, product.id)}
                  onClick={() => this.onClick(product.id)}
                  onSelectableInputChange={() => this.onClick(product.id)}
                  isSelected={selectedItems.includes(product.id)}
                >
                  <CardHeader>
                    <img src={icons[product.icon]} alt={`${product.name} icon`} style={{ maxWidth: '60px' }} />
                    <CardActions>
                      <Dropdown
                        isPlain
                        position="right"
                        onSelect={e => this.onCardKebabDropdownSelect(key, e)}
                        toggle={
                          <KebabToggle
                            onToggle={isCardKebabDropdownOpen =>
                              this.onCardKebabDropdownToggle(key, isCardKebabDropdownOpen)
                            }
                          />
                        }
                        isOpen={this.state[key]}
                        dropdownItems={[
                          <DropdownItem key="trash" onClick={this.deleteItem(product)} position="right">
                            <TrashIcon />
                            Delete
                          </DropdownItem>
                        ]}
                      />
                      <Checkbox
                        checked={isChecked}
                        value={product.id}
                        isChecked={selectedItems.includes(product.id)}
                        aria-label="card checkbox example"
                        id={`check-${product.id}`}
                      />
                    </CardActions>
                  </CardHeader>
                  <CardTitle>{product.name}</CardTitle>
                  <CardBody>{product.description}</CardBody>
                </Card>
              ))}
            </Gallery>
          </PageSection>
          <PageSection isFilled={false} sticky="bottom" padding={{ default: 'noPadding' }} variant="light">
            <Pagination
              itemCount={this.state.totalItemCount}
              page={page}
              page={this.state.page}
              perPage={this.state.perPage}
              onPerPageSelect={this.onPerPageSelect}
              onSetPage={this.onSetPage}
              variant="bottom"
            />
          </PageSection>
        </DashboardWrapper>
      </React.Fragment>
    );
  }
}
```
