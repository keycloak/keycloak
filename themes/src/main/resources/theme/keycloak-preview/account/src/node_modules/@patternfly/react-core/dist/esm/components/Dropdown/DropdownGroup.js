import _pt from "prop-types";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { DropdownContext } from './dropdownConstants';
export const DropdownGroup = ({
  children = null,
  className = '',
  label = ''
}) => React.createElement(DropdownContext.Consumer, null, ({
  sectionClass,
  sectionTitleClass,
  sectionComponent
}) => {
  const SectionComponent = sectionComponent;
  return React.createElement(SectionComponent, {
    className: css(sectionClass, className)
  }, label && React.createElement("h1", {
    className: css(sectionTitleClass),
    "aria-hidden": true
  }, label), React.createElement("ul", {
    role: "none"
  }, children));
});
DropdownGroup.propTypes = {
  children: _pt.node,
  className: _pt.string,
  label: _pt.node
};
//# sourceMappingURL=DropdownGroup.js.map