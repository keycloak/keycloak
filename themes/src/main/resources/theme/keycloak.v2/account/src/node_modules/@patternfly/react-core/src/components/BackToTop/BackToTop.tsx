import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/BackToTop/back-to-top';
import { css } from '@patternfly/react-styles';
import AngleUpIcon from '@patternfly/react-icons/dist/esm/icons/angle-up-icon';
import { canUseDOM } from '../../helpers/util';
import { Button } from '../Button';

interface BackToTopProps extends React.DetailedHTMLProps<React.HTMLProps<HTMLDivElement>, HTMLDivElement> {
  /** Additional classes added to the back to top. */
  className?: string;
  /** Title to appear in back to top button. */
  title?: string;
  /** Forwarded ref */
  innerRef?: React.Ref<any>;
  /** Selector for the scrollable element to spy on. Not passing a selector defaults to spying on window scroll events. */
  scrollableSelector?: string;
  /** Flag to always show back to top button, defaults to false. */
  isAlwaysVisible?: boolean;
}

const BackToTopBase: React.FunctionComponent<BackToTopProps> = ({
  className,
  title = 'Back to top',
  innerRef,
  scrollableSelector,
  isAlwaysVisible = false,
  ...props
}: BackToTopProps) => {
  const [visible, setVisible] = React.useState(isAlwaysVisible);
  React.useEffect(() => {
    setVisible(isAlwaysVisible);
  }, [isAlwaysVisible]);

  const [scrollElement, setScrollElement] = React.useState(null);

  const toggleVisible = () => {
    const scrolled = scrollElement.scrollY ? scrollElement.scrollY : scrollElement.scrollTop;
    if (!isAlwaysVisible) {
      if (scrolled > 400) {
        setVisible(true);
      } else {
        setVisible(false);
      }
    }
  };

  React.useEffect(() => {
    const hasScrollSpy = Boolean(scrollableSelector);
    if (hasScrollSpy) {
      const scrollEl = document.querySelector(scrollableSelector) as HTMLElement;
      if (!canUseDOM || !(scrollEl instanceof HTMLElement)) {
        return;
      }
      setScrollElement(scrollEl);
      scrollEl.addEventListener('scroll', toggleVisible);

      return () => {
        scrollEl.removeEventListener('scroll', toggleVisible);
      };
    } else {
      if (!canUseDOM) {
        return;
      }
      const scrollEl = window;
      setScrollElement(scrollEl);
      scrollEl.addEventListener('scroll', toggleVisible);

      return () => {
        scrollEl.removeEventListener('scroll', toggleVisible);
      };
    }
  }, [scrollableSelector, toggleVisible]);

  const handleClick = () => {
    scrollElement.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div
      className={css(styles.backToTop, !visible && styles.modifiers.hidden, className)}
      ref={innerRef}
      onClick={handleClick}
      {...props}
    >
      <Button variant="primary" icon={<AngleUpIcon aria-hidden="true" />} iconPosition="right">
        {title}
      </Button>
    </div>
  );
};

export const BackToTop = React.forwardRef((props: BackToTopProps, ref: React.Ref<any>) => (
  <BackToTopBase innerRef={ref} {...props} />
));
BackToTop.displayName = 'BackToTop';
