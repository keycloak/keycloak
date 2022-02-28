import React from 'react';
import PropTypes from 'prop-types';

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
        <div className="pf-c-empty-state pf-m-lg">
          <i className="fas fa- fa-cogs pf-c-empty-state__icon" aria-hidden="true" />
          <h1 className="pf-c-title pf-m-lg">
            {percent === 100 ? 'Configuration Complete' : 'Configuration in progress'}
          </h1>
          <div className="pf-c-empty-state__body">
            <div className="pf-c-progress pf-m-singleline" id="progress-singleline-example">
              <div className="pf-c-progress__description" id="progress-singleline-example-description" />
              <div className="pf-c-progress__status" aria-hidden="true">
                <span className="pf-c-progress__measure">{percent}%</span>
              </div>
              <div
                className="pf-c-progress__bar"
                role="progressbar"
                aria-valuemin="0"
                aria-valuemax="100"
                aria-valuenow={percent}
                aria-describedby="progress-singleline-example-description"
              >
                <div
                  className="pf-c-progress__indicator"
                  style={{
                    width: `${percent}%`
                  }}
                />
              </div>
            </div>
          </div>
          <div className="pf-c-empty-state__body">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec non pulvinar tortor. Maecenas sit amet
            pellentesque velit, eu eleifend mauris.
          </div>
          <div className="pf-c-empty-state__secondary">
            <button
              className={percent === 100 ? 'pf-c-button pf-m-primary' : 'pf-c-button pf-m-link'}
              onClick={this.props.onClose}
            >
              {percent === 100 ? 'Close' : 'Cancel'}
            </button>
          </div>
        </div>
      </div>
    );
  }
}

FinishedStep.propTypes = propTypes;

export default FinishedStep;
