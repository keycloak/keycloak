import _pt from "prop-types";

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { DataToolbarItem } from './DataToolbarItem';
import { ChipGroup, Chip, ChipGroupToolbarItem } from '../../components/ChipGroup';
import { DataToolbarContentContext, DataToolbarContext } from './DataToolbarUtils';
export class DataToolbarFilter extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isMounted: false
    };
  }

  componentDidMount() {
    const {
      categoryName,
      chips
    } = this.props;
    this.context.updateNumberFilters(typeof categoryName === 'string' ? categoryName : categoryName.name, chips.length);
    this.setState({
      isMounted: true
    });
  }

  componentDidUpdate() {
    const {
      categoryName,
      chips
    } = this.props;
    this.context.updateNumberFilters(typeof categoryName === 'string' ? categoryName : categoryName.name, chips.length);
  }

  render() {
    const _this$props = this.props,
          {
      children,
      chips,
      deleteChip,
      categoryName,
      showToolbarItem
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "chips", "deleteChip", "categoryName", "showToolbarItem"]);

    const {
      isExpanded,
      chipGroupContentRef
    } = this.context;
    const chipGroup = chips.length ? React.createElement(DataToolbarItem, {
      variant: "chip-group"
    }, React.createElement(ChipGroup, {
      withToolbar: true
    }, React.createElement(ChipGroupToolbarItem, {
      key: typeof categoryName === 'string' ? categoryName : categoryName.key,
      categoryName: typeof categoryName === 'string' ? categoryName : categoryName.name
    }, chips.map(chip => typeof chip === 'string' ? React.createElement(Chip, {
      key: chip,
      onClick: () => deleteChip(categoryName, chip)
    }, chip) : React.createElement(Chip, {
      key: chip.key,
      onClick: () => deleteChip(categoryName, chip)
    }, chip.node))))) : null;

    if (!isExpanded && this.state.isMounted) {
      return React.createElement(React.Fragment, null, showToolbarItem && React.createElement(DataToolbarItem, props, children), ReactDOM.createPortal(chipGroup, chipGroupContentRef.current.firstElementChild));
    }

    return React.createElement(DataToolbarContentContext.Consumer, null, ({
      chipContainerRef
    }) => React.createElement(React.Fragment, null, showToolbarItem && React.createElement(DataToolbarItem, props, children), chipContainerRef.current && ReactDOM.createPortal(chipGroup, chipContainerRef.current)));
  }

}

_defineProperty(DataToolbarFilter, "propTypes", {
  chips: _pt.arrayOf(_pt.oneOfType([_pt.string, _pt.shape({
    key: _pt.string.isRequired,
    node: _pt.node.isRequired
  })])),
  deleteChip: _pt.func,
  children: _pt.node.isRequired,
  categoryName: _pt.oneOfType([_pt.string, _pt.shape({
    key: _pt.string.isRequired,
    name: _pt.string.isRequired
  })]).isRequired,
  showToolbarItem: _pt.bool
});

_defineProperty(DataToolbarFilter, "contextType", DataToolbarContext);

_defineProperty(DataToolbarFilter, "defaultProps", {
  chips: [],
  showToolbarItem: true
});
//# sourceMappingURL=DataToolbarFilter.js.map