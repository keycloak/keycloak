import React from 'react';
import PropTypes from 'prop-types';
import {
  EmptyState,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions,
  Title,
  Progress,
  Button
} from '@patternfly/react-core';
// eslint-disable-next-line patternfly-react/import-tokens-icons
import { CogsIcon } from '@patternfly/react-icons';

const propTypes = {
  onClose: PropTypes.func.isRequired
};

class FinishedStep extends React.Component {
  constructor(props) {
    super(props);
    this.state = { percent: 0 };
  }

  tick() {
    if (this.state.percent < 100) {
      this.setState(prevState => ({
        percent: prevState.percent + 20
      }));
    }
  }

  componentDidMount() {
    this.interval = setInterval(() => this.tick(), 1000);
  }

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  render() {
    const { percent } = this.state;
    return (
      <div className="pf-l-bullseye">
        <EmptyState variant="large">
          <EmptyStateIcon icon={CogsIcon} />
          <Title headingLevel="h4" size="lg">
            {percent === 100 ? 'Validation complete' : 'Validating credentials'}
          </Title>
          <EmptyStateBody>
            <Progress value={percent} measureLocation="outside" />
          </EmptyStateBody>
          <EmptyStateBody>
            Description can be used to further elaborate on the validation step, or give the user a better idea of how
            long the process will take.
          </EmptyStateBody>
          <EmptyStateSecondaryActions>
            <Button isDisabled={percent !== 100} onClick={this.props.onClose}>
              Log to console
            </Button>
          </EmptyStateSecondaryActions>
        </EmptyState>
      </div>
    );
  }
}

FinishedStep.propTypes = propTypes;

export default FinishedStep;
