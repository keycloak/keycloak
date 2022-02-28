import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ChipGroup/chip-group';
import { css } from '@patternfly/react-styles';
import { Chip } from './Chip';
import { fillTemplate } from '../../helpers';
export const ChipGroupContext = React.createContext('');
export class ChipGroup extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "toggleCollapse", () => {
      this.setState(prevState => ({
        isOpen: !prevState.isOpen
      }));
    });

    this.state = {
      isOpen: this.props.defaultIsOpen
    };
  }

  renderToolbarGroup() {
    const {
      isOpen
    } = this.state;
    const {
      headingLevel = 'h4'
    } = this.props;
    return React.createElement(ChipGroupContext.Provider, {
      value: headingLevel
    }, React.createElement(InnerChipGroup, _extends({}, this.props, {
      isOpen: isOpen,
      onToggleCollapse: this.toggleCollapse
    })));
  }

  renderChipGroup() {
    const {
      className
    } = this.props;
    const {
      isOpen
    } = this.state;
    return React.createElement("ul", {
      className: css(styles.chipGroup, className)
    }, React.createElement(InnerChipGroup, _extends({}, this.props, {
      isOpen: isOpen,
      onToggleCollapse: this.toggleCollapse
    })));
  }

  render() {
    const {
      withToolbar,
      children
    } = this.props;

    if (React.Children.count(children)) {
      return withToolbar ? this.renderToolbarGroup() : this.renderChipGroup();
    }

    return null;
  }

}

_defineProperty(ChipGroup, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  defaultIsOpen: _pt.bool,
  expandedText: _pt.string,
  collapsedText: _pt.string,
  withToolbar: _pt.bool,
  headingLevel: _pt.oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6']),
  numChips: _pt.number
});

_defineProperty(ChipGroup, "defaultProps", {
  className: '',
  expandedText: 'Show Less',
  collapsedText: '${remaining} more',
  withToolbar: false,
  defaultIsOpen: false,
  numChips: 3
});

const InnerChipGroup = props => {
  const {
    children,
    expandedText,
    isOpen,
    onToggleCollapse,
    collapsedText,
    withToolbar,
    numChips
  } = props;
  const collapsedTextResult = fillTemplate(collapsedText, {
    remaining: React.Children.count(children) - numChips
  });
  const mappedChildren = React.Children.map(children, c => {
    const child = c;

    if (withToolbar) {
      return React.cloneElement(child, {
        children: React.Children.toArray(child.props.children).map(chip => React.cloneElement(chip, {
          component: 'li'
        }))
      });
    }

    return React.cloneElement(child, {
      component: 'li'
    });
  });
  return React.createElement(React.Fragment, null, isOpen ? React.createElement(React.Fragment, null, mappedChildren) : React.createElement(React.Fragment, null, mappedChildren.map((child, i) => {
    if (i < numChips) {
      return child;
    }
  })), React.Children.count(children) > numChips && React.createElement(Chip, {
    isOverflowChip: true,
    onClick: onToggleCollapse,
    component: withToolbar ? 'div' : 'li'
  }, isOpen ? expandedText : collapsedTextResult));
};

InnerChipGroup.propTypes = {
  children: _pt.node,
  className: _pt.string,
  defaultIsOpen: _pt.bool,
  expandedText: _pt.string,
  collapsedText: _pt.string,
  withToolbar: _pt.bool,
  headingLevel: _pt.oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6']),
  numChips: _pt.number,
  isOpen: _pt.bool.isRequired,
  onToggleCollapse: _pt.func.isRequired
};
//# sourceMappingURL=ChipGroup.js.map