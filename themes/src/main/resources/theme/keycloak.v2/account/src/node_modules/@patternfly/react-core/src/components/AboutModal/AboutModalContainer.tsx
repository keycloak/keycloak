import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Bullseye/bullseye';
import { FocusTrap } from '../../helpers';

import { AboutModalBoxContent } from './AboutModalBoxContent';
import { AboutModalBoxHeader } from './AboutModalBoxHeader';
import { AboutModalBoxHero } from './AboutModalBoxHero';
import { AboutModalBoxBrand } from './AboutModalBoxBrand';
import { AboutModalBoxCloseButton } from './AboutModalBoxCloseButton';
import { AboutModalBox } from './AboutModalBox';
import { Backdrop } from '../Backdrop/Backdrop';

export interface AboutModalContainerProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the About Modal Box Content.  */
  children: React.ReactNode;
  /** additional classes added to the About Modal Box  */
  className?: string;
  /** Flag to show the About Modal  */
  isOpen?: boolean;
  /** A callback for when the close button is clicked  */
  onClose?: () => void;
  /** Product Name  */
  productName?: string;
  /** Trademark information  */
  trademark?: string;
  /** the URL of the image for the Brand.  */
  brandImageSrc: string;
  /** the alternate text of the Brand image.  */
  brandImageAlt: string;
  /** the URL of the image for the background.  */
  backgroundImageSrc?: string;
  /** id to use for About Modal Box aria labeled by  */
  aboutModalBoxHeaderId: string;
  /** id to use for About Modal Box aria described by  */
  aboutModalBoxContentId: string;
  /** Set close button aria label */
  closeButtonAriaLabel?: string;
  /** Flag to disable focus trap */
  disableFocusTrap?: boolean;
}

export const AboutModalContainer: React.FunctionComponent<AboutModalContainerProps> = ({
  children,
  className = '',
  isOpen = false,
  onClose = () => undefined,
  productName = '',
  trademark,
  brandImageSrc,
  brandImageAlt,
  backgroundImageSrc,
  closeButtonAriaLabel,
  aboutModalBoxHeaderId,
  aboutModalBoxContentId,
  disableFocusTrap = false,
  ...props
}: AboutModalContainerProps) => {
  if (!isOpen) {
    return null;
  }
  return (
    <Backdrop>
      <FocusTrap
        active={!disableFocusTrap}
        focusTrapOptions={{ clickOutsideDeactivates: true, tabbableOptions: { displayCheck: 'none' } }}
        className={css(styles.bullseye)}
      >
        <AboutModalBox
          className={className}
          aria-labelledby={aboutModalBoxHeaderId}
          aria-describedby={aboutModalBoxContentId}
        >
          <AboutModalBoxBrand src={brandImageSrc} alt={brandImageAlt} />
          <AboutModalBoxCloseButton aria-label={closeButtonAriaLabel} onClose={onClose} />
          {productName && <AboutModalBoxHeader id={aboutModalBoxHeaderId} productName={productName} />}
          <AboutModalBoxContent
            trademark={trademark}
            id={aboutModalBoxContentId}
            noAboutModalBoxContentContainer={false}
            {...props}
          >
            {children}
          </AboutModalBoxContent>
          <AboutModalBoxHero backgroundImageSrc={backgroundImageSrc} />
        </AboutModalBox>
      </FocusTrap>
    </Backdrop>
  );
};
AboutModalContainer.displayName = 'AboutModalContainer';
