---
id: Toolbar
section: components
---

import DashboardWrapper from './examples/DashboardWrapper';

import PauseIcon from '@patternfly/react-icons/dist/esm/icons/pause-icon';
import PlayIcon from '@patternfly/react-icons/dist/esm/icons/play-icon';
import ExpandIcon from '@patternfly/react-icons/dist/esm/icons/expand-icon';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-alt-icon';
import DownloadIcon from '@patternfly/react-icons/dist/esm/icons/download-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';

## Demos

### Console log viewer toolbar demo

This is an example of toolbar usage in log viewer.

```js isFullscreen
import React from 'react';
import { Toolbar, ToolbarContent, ToolbarGroup, ToolbarItem, ToolbarToggleGroup } from '@patternfly/react-core';
import {
  Badge,
  Button,
  Checkbox,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  DropdownToggleAction,
  DropdownPosition,
  DropdownSeparator,
  SearchInput,
  Select,
  SelectOption,
  PageSection,
  PageSectionVariants,
  Tooltip
} from '@patternfly/react-core';
import DashboardWrapper from './examples/DashboardWrapper';

import PauseIcon from '@patternfly/react-icons/dist/esm/icons/pause-icon';
import PlayIcon from '@patternfly/react-icons/dist/esm/icons/play-icon';
import ExpandIcon from '@patternfly/react-icons/dist/esm/icons/expand-icon';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-alt-icon';
import DownloadIcon from '@patternfly/react-icons/dist/esm/icons/download-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';

class ConsoleLogViewerToolbar extends React.Component {
  constructor(props) {
    super(props);

    this.firstOptions = {
      'container-sample-1': { type: 'C' },
      'container-sample-2': { type: 'D' },
      'container-sample-3': { type: 'E' }
    };

    this.state = {
      containerExpanded: false,
      containerExpandedMobile: false,
      containerSelected: Object.keys(this.firstOptions)[0],
      optionExpanded: false,
      optionExpandedMobile: false,
      isPaused: false,
      firstSwitchChecked: true,
      secondSwitchChecked: false,
      searchValue: '',
      searchResultsCount: 3,
      currentSearchResult: 1,
      externalExpanded: false,
      externalExpandedMobile: false,
      downloadExpanded: false,
      downloadExpandedMobile: false,
      mobileView: window.innerWidth >= 1450 ? false : true
    };

    this.onContainerToggle = isExpanded => {
      this.setState({
        containerExpanded: isExpanded
      });
    };

    this.onContainerToggleMobile = isExpanded => {
      this.setState({
        containerExpandedMobile: isExpanded
      });
    };

    this.onContainerSelect = (event, selection) => {
      this.setState({
        containerSelected: selection,
        containerExpanded: false
      });
    };

    this.onContainerSelectMobile = (event, selection) => {
      this.setState({
        containerSelected: selection,
        containerExpandedMobile: false
      });
    };

    this.onOptionToggle = isExpanded => {
      this.setState({
        optionExpanded: isExpanded
      });
    };

    this.onOptionToggleMobile = isExpanded => {
      this.setState({
        optionExpandedMobile: isExpanded
      });
    };

    this.onOptionSelect = event => {
      this.setState({
        optionExpanded: !this.state.optionExpanded
      });
    };

    this.onOptionSelectMobile = event => {
      this.setState({
        optionExpandedMobile: !this.state.optionExpandedMobile
      });
    };

    this.onExternalToggle = isExpanded => {
      this.setState({
        externalExpanded: isExpanded
      });
    };

    this.onExternalToggleMobile = isExpanded => {
      this.setState({
        externalExpandedMobile: isExpanded
      });
    };

    this.onExternalSelect = event => {
      this.setState({
        externalExpanded: !this.state.externalExpanded
      });
    };

    this.onExternalSelectMobile = event => {
      this.setState({
        externalExpandedMobile: !this.state.externalExpandedMobile
      });
    };

    this.onDownloadToggle = isExpanded => {
      this.setState({
        downloadExpanded: isExpanded
      });
    };

    this.onDownloadToggleMobile = isExpanded => {
      this.setState({
        downloadExpandedMobile: isExpanded
      });
    };

    this.onDownloadSelect = event => {
      this.setState({
        downloadExpanded: !this.state.downloadExpanded
      });
    };

    this.onDownloadSelectMobile = event => {
      this.setState({
        downloadExpandedMobile: !this.state.downloadExpandedMobile
      });
    };

    this.onExternalClick = event => {
      window.alert('Open external logs!');
    };

    this.onDownloadClick = event => {
      window.alert('Action!');
    };

    this.pauseOrStart = event => {
      this.setState({
        isPaused: !this.state.isPaused
      });
    };

    this.handleFirstSwitchChange = (firstSwitchChecked, event) => {
      this.setState({ firstSwitchChecked });
    };

    this.handleSecondSwitchChange = (secondSwitchChecked, event) => {
      this.setState({ secondSwitchChecked });
    };

    this.onClearLogs = event => {
      this.setState({
        optionExpanded: false
      });
      window.alert('Clear Logs!');
    };

    this.onSearchChange = (value, event) => {
      this.setState({
        searchValue: value,
        searchResultsCount: 3
      });
    };

    this.onSearchClear = event => {
      this.setState({
        searchValue: '',
        searchResultsCount: 0,
        currentSearchResult: 1
      });
    };

    this.onSearchNext = event => {
      this.setState(prevState => {
        const newCurrentResult = prevState.currentSearchResult + 1;
        return {
          currentSearchResult:
            newCurrentResult <= prevState.searchResultsCount ? newCurrentResult : prevState.searchResultsCount
        };
      });
    };

    this.onSearchPrevious = event => {
      this.setState(prevState => {
        const newCurrentResult = prevState.currentSearchResult - 1;
        return {
          currentSearchResult: newCurrentResult > 0 ? newCurrentResult : 1
        };
      });
    };

    this.onPageResize = ({ windowSize }) => {
      if (windowSize >= 1450) {
        this.setState({
          mobileView: false
        });
      } else {
        this.setState({
          mobileView: true
        });
      }
    };
  }

  render() {
    const {
      containerExpanded,
      containerExpandedMobile,
      containerSelected,
      optionExpanded,
      optionExpandedMobile,
      isPaused,
      firstSwitchChecked,
      secondSwitchChecked,
      searchValue,
      searchResultsCount,
      currentSearchResult,
      externalExpanded,
      externalExpandedMobile,
      downloadExpanded,
      downloadExpandedMobile,
      kebabExpanded,
      mobileView
    } = this.state;

    const externalDropdownItems = [
      <DropdownItem key="action-1" component="button" onClick={this.onExternalClick}>
        External logs
      </DropdownItem>,
      <DropdownItem key="action-2" component="button" onClick={this.onExternalClick}>
        External logs
      </DropdownItem>,
      <DropdownItem key="action-3" component="button" onClick={this.onExternalClick}>
        External logs
      </DropdownItem>
    ];

    const downloadDropdownItems = [
      <DropdownItem key="action-4" component="button" onClick={this.onDownloadClick}>
        Current container logs
      </DropdownItem>,
      <DropdownItem key="action-5" component="button" onClick={this.onDownloadClick}>
        All container logs
      </DropdownItem>
    ];

    const optionDropdownItems = [
      <DropdownItem key="switch-1">
        <Checkbox
          label="Display timestamp"
          isChecked={firstSwitchChecked}
          onChange={this.handleFirstSwitchChange}
          id="switchMobileCheckbox1"
          name="check1"
        />
      </DropdownItem>,
      <DropdownItem key="switch-2">
        <Checkbox
          label="Wrap lines"
          isChecked={secondSwitchChecked}
          onChange={this.handleSecondSwitchChange}
          id="switchMobileCheckbox2"
          name="check2"
        />
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem key="clear-log" onClick={this.onClearLogs}>
        Clear logs
      </DropdownItem>
    ];

    const selectDropdownContent = (
      <React.Fragment>
        {Object.entries(this.firstOptions).map(([value, { type }]) => (
          <SelectOption
            key={value}
            value={value}
            isSelected={containerSelected === value}
            isChecked={containerSelected === value}
          >
            <Badge key={value}>{type}</Badge>
            {` ${value}`}
          </SelectOption>
        ))}
      </React.Fragment>
    );

    const selectToggleContent = ({ showText }) => {
      if (!containerSelected) {
        return 'Select';
      }
      return (
        <React.Fragment>
          <Badge>{this.firstOptions[containerSelected].type}</Badge>
          {showText && ` ${containerSelected}`}
        </React.Fragment>
      );
    };

    const LogsSearchInput = (
      <ToolbarToggleGroup toggleIcon={<SearchIcon />} breakpoint="lg">
        <ToolbarItem>
          <SearchInput
            placeholder="Search logs"
            value={searchValue}
            onChange={this.onSearchChange}
            onClear={this.onSearchClear}
            resultsCount={`${currentSearchResult} / ${searchResultsCount}`}
            onNextClick={this.onSearchNext}
            onPreviousClick={this.onSearchPrevious}
          />
        </ToolbarItem>
      </ToolbarToggleGroup>
    );

    const leftAlignedItemsDesktop = (
      <React.Fragment>
        <ToolbarItem visibility={{ default: 'hidden', '2xl': 'visible' }}>
          <Select
            onToggle={this.onContainerToggle}
            onSelect={this.onContainerSelect}
            selections={containerSelected}
            isOpen={containerExpanded}
            customContent={selectDropdownContent}
            placeholderText={selectToggleContent({ showText: true })}
          />
        </ToolbarItem>
        <ToolbarItem visibility={{ default: 'hidden', '2xl': 'visible' }}>
          <Dropdown
            toggle={
              <DropdownToggle id="option-toggle-desktop" onToggle={this.onOptionToggle} icon={<CogIcon />}>
                Options
              </DropdownToggle>
            }
            isOpen={optionExpanded}
            dropdownItems={optionDropdownItems}
          />
        </ToolbarItem>
        <ToolbarItem visibility={{ default: 'hidden', '2xl': 'visible' }}>
          <Button variant={isPaused ? 'plain' : 'link'} onClick={this.pauseOrStart}>
            {isPaused ? <PlayIcon /> : <PauseIcon />}
            {isPaused ? ` Resume Log` : ` Pause Log`}
          </Button>
        </ToolbarItem>
      </React.Fragment>
    );

    const leftAlignedItemsMobile = (
      <React.Fragment>
        <ToolbarItem visibility={{ default: 'visible', '2xl': 'hidden' }}>
          <Tooltip position="top" content={<div>Select container</div>}>
            <Select
              onToggle={this.onContainerToggleMobile}
              onSelect={this.onContainerSelectMobile}
              selections={containerSelected}
              isOpen={containerExpandedMobile}
              customContent={selectDropdownContent}
              placeholderText={selectToggleContent({ showText: false })}
            />
          </Tooltip>
        </ToolbarItem>
        <ToolbarItem visibility={{ default: 'visible', '2xl': 'hidden' }}>
          <Tooltip position="top" content={<div>Options</div>}>
            <Dropdown
              toggle={
                <DropdownToggle aria-label="Options" id="option-toggle-mobile" onToggle={this.onOptionToggleMobile} icon={<CogIcon />} />
              }
              isOpen={optionExpandedMobile}
              dropdownItems={optionDropdownItems}
            />
          </Tooltip>
        </ToolbarItem>
        <ToolbarItem visibility={{ default: 'visible', '2xl': 'hidden' }}>
          <Tooltip position="top" content={<div>{isPaused ? 'Resume log' : 'Pause log'}</div>}>
            <Button variant="plain" onClick={this.pauseOrStart} aria-label={isPaused ? 'Play' : 'Paused'}>
              {isPaused ? <PlayIcon /> : <PauseIcon />}
            </Button>
          </Tooltip>
        </ToolbarItem>
      </React.Fragment>
    );

    const leftAlignedItems = (
      <React.Fragment>
        {leftAlignedItemsDesktop}
        {leftAlignedItemsMobile}
      </React.Fragment>
    );

    const rightAlignedItemsDesktop = (
      <React.Fragment>
        <ToolbarItem visibility={{ default: 'hidden', '2xl': 'visible' }}>
          <Dropdown
            onSelect={this.onExternalSelect}
            toggle={
              <DropdownToggle id="external-toggle" onToggle={this.onExternalToggle}>
                External logs
              </DropdownToggle>
            }
            isOpen={externalExpanded}
            dropdownItems={externalDropdownItems}
          />
        </ToolbarItem>
        <ToolbarItem visibility={{ default: 'hidden', '2xl': 'visible' }}>
          <Dropdown
            onSelect={this.onDownloadSelect}
            toggle={
              <DropdownToggle id="download-toggle" onToggle={this.onDownloadToggle}>
                Download
              </DropdownToggle>
            }
            isOpen={downloadExpanded}
            dropdownItems={downloadDropdownItems}
          />
        </ToolbarItem>
      </React.Fragment>
    );

    const rightAlignedItemsMobile = (
      <React.Fragment>
        <ToolbarItem visibility={{ default: 'visible', '2xl': 'hidden' }}>
          <Tooltip position="top" content={<div>External logs</div>}>
            <Dropdown
              onSelect={this.onExternalSelectMobile}
              toggle={
                <DropdownToggle
                  id="mobile-external-toggle"
                  onToggle={this.onExternalToggleMobile}
                  aria-label="External logs"
                  icon={<ExternalLinkAltIcon />}
                />
              }
              isOpen={externalExpandedMobile}
              dropdownItems={externalDropdownItems}
            />
          </Tooltip>
        </ToolbarItem>
        <ToolbarItem visibility={{ default: 'visible', '2xl': 'hidden' }}>
          <Tooltip position="top" content={<div>Download</div>}>
            <Dropdown
              onSelect={this.onDownloadSelectMobile}
              toggle={
                <DropdownToggle
                  id="mobile-download-toggle"
                  aria-label="Download"
                  onToggle={this.onDownloadToggleMobile}
                  icon={<DownloadIcon />}
                />
              }
              isOpen={downloadExpandedMobile}
              position={DropdownPosition.right}
              dropdownItems={downloadDropdownItems}
            />
          </Tooltip>
        </ToolbarItem>
      </React.Fragment>
    );

    const rightAlignedItems = (
      <React.Fragment>
        <ToolbarItem>{LogsSearchInput}</ToolbarItem>
        {rightAlignedItemsDesktop}
        {rightAlignedItemsMobile}
        <ToolbarItem>
          <Tooltip position="top" content={<div>Expand</div>}>
            <Button variant="plain" aria-label="expand">
              <ExpandIcon />
            </Button>
          </Tooltip>
        </ToolbarItem>
      </React.Fragment>
    );

    const items = (
      <React.Fragment>
        <ToolbarGroup alignment={{ default: 'alignLeft' }}>{leftAlignedItems}</ToolbarGroup>
        <ToolbarGroup alignment={{ default: 'alignRight' }}>{rightAlignedItems}</ToolbarGroup>
      </React.Fragment>
    );

    const toolbar = (
      <Toolbar
        id="log-viewer-toolbar"
        inset={{
          default: 'insetNone'
        }}
      >
        <ToolbarContent>{items}</ToolbarContent>
      </Toolbar>
    );
    return (
      <DashboardWrapper sidebarNavOpen={!mobileView} onPageResize={this.onPageResize}>
        <PageSection variant={PageSectionVariants.light}>{toolbar}</PageSection>
      </DashboardWrapper>
    );
  }
}
```
