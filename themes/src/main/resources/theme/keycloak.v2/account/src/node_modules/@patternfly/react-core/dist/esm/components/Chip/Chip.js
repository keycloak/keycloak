import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { Button } from '../Button';
import { Tooltip } from '../Tooltip';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import styles from '@patternfly/react-styles/css/components/Chip/chip';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
import { getOUIAProps, getDefaultOUIAId } from '../../helpers';
export class Chip extends React.Component {
    constructor(props) {
        super(props);
        this.span = React.createRef();
        this.setChipStyle = () => ({
            '--pf-c-chip__text--MaxWidth': this.props.textMaxWidth
        });
        this.renderOverflowChip = () => {
            const { children, className, onClick, ouiaId } = this.props;
            const Component = this.props.component;
            return (React.createElement(Component, Object.assign({ onClick: onClick }, (this.props.textMaxWidth && Object.assign({ style: this.setChipStyle() }, this.props.style)), { className: css(styles.chip, styles.modifiers.overflow, className) }, (this.props.component === 'button' ? { type: 'button' } : {}), getOUIAProps('OverflowChip', ouiaId !== undefined ? ouiaId : this.state.ouiaStateId)),
                React.createElement("span", { className: css(styles.chipText) }, children)));
        };
        this.renderChip = (randomId) => {
            const { children, tooltipPosition } = this.props;
            if (this.state.isTooltipVisible) {
                return (React.createElement(Tooltip, { position: tooltipPosition, content: children }, this.renderInnerChip(randomId)));
            }
            return this.renderInnerChip(randomId);
        };
        this.state = {
            isTooltipVisible: false,
            ouiaStateId: getDefaultOUIAId(Chip.displayName)
        };
    }
    componentDidMount() {
        this.setState({
            isTooltipVisible: Boolean(this.span.current && this.span.current.offsetWidth < this.span.current.scrollWidth)
        });
    }
    renderInnerChip(id) {
        const { children, className, onClick, closeBtnAriaLabel, isReadOnly, component, ouiaId } = this.props;
        const Component = component;
        return (React.createElement(Component, Object.assign({}, (this.props.textMaxWidth && {
            style: this.setChipStyle()
        }), { className: css(styles.chip, className) }, (this.state.isTooltipVisible && { tabIndex: 0 }), getOUIAProps(Chip.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId)),
            React.createElement("span", { ref: this.span, className: css(styles.chipText), id: id }, children),
            !isReadOnly && (React.createElement(Button, { onClick: onClick, variant: "plain", "aria-label": closeBtnAriaLabel, id: `remove_${id}`, "aria-labelledby": `remove_${id} ${id}`, ouiaId: ouiaId || closeBtnAriaLabel },
                React.createElement(TimesIcon, { "aria-hidden": "true" })))));
    }
    render() {
        const { isOverflowChip } = this.props;
        return (React.createElement(GenerateId, null, randomId => (isOverflowChip ? this.renderOverflowChip() : this.renderChip(this.props.id || randomId))));
    }
}
Chip.displayName = 'Chip';
Chip.defaultProps = {
    closeBtnAriaLabel: 'close',
    className: '',
    isOverflowChip: false,
    isReadOnly: false,
    tooltipPosition: 'top',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: (_e) => undefined,
    component: 'div'
};
//# sourceMappingURL=Chip.js.map