import * as React from 'react';
import { FocusTrap } from '../../helpers';
import modalStyles from '@patternfly/react-styles/css/components/ModalBox/modal-box';
import bullsEyeStyles from '@patternfly/react-styles/css/layouts/Bullseye/bullseye';
import { css } from '@patternfly/react-styles';
import { getOUIAProps, OUIAProps } from '../../helpers';

import { Backdrop } from '../Backdrop/Backdrop';
import { ModalBoxBody } from './ModalBoxBody';
import { ModalBoxCloseButton } from './ModalBoxCloseButton';
import { ModalBox } from './ModalBox';
import { ModalBoxFooter } from './ModalBoxFooter';
import { ModalBoxDescription } from './ModalBoxDescription';
import { ModalBoxHeader } from './ModalBoxHeader';
import { ModalBoxTitle, isVariantIcon } from './ModalBoxTitle';

export interface ModalContentProps extends OUIAProps {
  /** Content rendered inside the Modal. */
  children: React.ReactNode;
  /** Additional classes added to the button */
  className?: string;
  /** Variant of the modal */
  variant?: 'small' | 'medium' | 'large' | 'default';
  /** Alternate position of the modal */
  position?: 'top';
  /** Offset from alternate position. Can be any valid CSS length/percentage */
  positionOffset?: string;
  /** Flag to show the modal */
  isOpen?: boolean;
  /** Complex header (more than just text), supersedes title for header content */
  header?: React.ReactNode;
  /** Optional help section for the Modal Header */
  help?: React.ReactNode;
  /** Description of the modal */
  description?: React.ReactNode;
  /** Simple text content of the Modal Header, also used for aria-label on the body */
  title?: string;
  /** Optional alert icon (or other) to show before the title of the Modal Header
   * When the predefined alert types are used the default styling
   * will be automatically applied */
  titleIconVariant?: 'success' | 'danger' | 'warning' | 'info' | 'default' | React.ComponentType<any>;
  /** Optional title label text for screen readers */
  titleLabel?: string;
  /** Id of Modal Box label */
  'aria-labelledby'?: string | null;
  /** Accessible descriptor of modal */
  'aria-label'?: string;
  /** Id of Modal Box description */
  'aria-describedby'?: string;
  /** Accessible label applied to the modal box body. This should be used to communicate important information about the modal box body div if needed, such as that it is scrollable */
  bodyAriaLabel?: string;
  /** Accessible role applied to the modal box body. This will default to region if a body aria label is applied. Set to a more appropriate role as applicable based on the modal content and context */
  bodyAriaRole?: string;
  /** Flag to show the close button in the header area of the modal */
  showClose?: boolean;
  /** Default width of the content. */
  width?: number | string;
  /** Custom footer */
  footer?: React.ReactNode;
  /** Action buttons to add to the standard Modal Footer, ignored if `footer` is given */
  actions?: any;
  /** A callback for when the close button is clicked */
  onClose?: () => void;
  /** Id of the ModalBox container */
  boxId: string;
  /** Id of the ModalBox title */
  labelId: string;
  /** Id of the ModalBoxBody */
  descriptorId: string;
  /** Flag to disable focus trap */
  disableFocusTrap?: boolean;
  /** Flag indicating if modal content should be placed in a modal box body wrapper */
  hasNoBodyWrapper?: boolean;
}

export const ModalContent: React.FunctionComponent<ModalContentProps> = ({
  children,
  className = '',
  isOpen = false,
  header = null,
  help = null,
  description = null,
  title = '',
  titleIconVariant = null,
  titleLabel = '',
  'aria-label': ariaLabel = '',
  'aria-describedby': ariaDescribedby,
  'aria-labelledby': ariaLabelledby,
  bodyAriaLabel,
  bodyAriaRole,
  showClose = true,
  footer = null,
  actions = [],
  onClose = () => undefined as any,
  variant = 'default',
  position,
  positionOffset,
  width = -1,
  boxId,
  labelId,
  descriptorId,
  disableFocusTrap = false,
  hasNoBodyWrapper = false,
  ouiaId,
  ouiaSafe = true,
  ...props
}: ModalContentProps) => {
  if (!isOpen) {
    return null;
  }

  const modalBoxHeader = header ? (
    <ModalBoxHeader help={help}>{header}</ModalBoxHeader>
  ) : (
    title && (
      <ModalBoxHeader help={help}>
        <ModalBoxTitle title={title} titleIconVariant={titleIconVariant} titleLabel={titleLabel} id={labelId} />
        {description && <ModalBoxDescription id={descriptorId}>{description}</ModalBoxDescription>}
      </ModalBoxHeader>
    )
  );

  const modalBoxFooter = footer ? (
    <ModalBoxFooter>{footer}</ModalBoxFooter>
  ) : (
    actions.length > 0 && <ModalBoxFooter>{actions}</ModalBoxFooter>
  );

  const defaultModalBodyAriaRole = bodyAriaLabel ? 'region' : undefined;

  const modalBody = hasNoBodyWrapper ? (
    children
  ) : (
    <ModalBoxBody
      aria-label={bodyAriaLabel}
      role={bodyAriaRole || defaultModalBodyAriaRole}
      {...props}
      {...(!description && !ariaDescribedby && { id: descriptorId })}
    >
      {children}
    </ModalBoxBody>
  );
  const boxStyle = width === -1 ? {} : { width };
  const ariaLabelledbyFormatted = (): null | string => {
    if (ariaLabelledby === null) {
      return null;
    }
    const idRefList: string[] = [];
    if ((ariaLabel && boxId) !== '') {
      idRefList.push(ariaLabel && boxId);
    }
    if (ariaLabelledby) {
      idRefList.push(ariaLabelledby);
    }
    if (title) {
      idRefList.push(labelId);
    }
    return idRefList.join(' ');
  };

  const modalBox = (
    <ModalBox
      id={boxId}
      style={boxStyle}
      className={css(
        className,
        isVariantIcon(titleIconVariant) &&
          modalStyles.modifiers[titleIconVariant as 'success' | 'warning' | 'info' | 'danger' | 'default']
      )}
      variant={variant}
      position={position}
      positionOffset={positionOffset}
      aria-label={ariaLabel}
      aria-labelledby={ariaLabelledbyFormatted()}
      aria-describedby={ariaDescribedby || (hasNoBodyWrapper ? null : descriptorId)}
      {...getOUIAProps(ModalContent.displayName, ouiaId, ouiaSafe)}
    >
      {showClose && <ModalBoxCloseButton onClose={onClose} ouiaId={ouiaId} />}
      {modalBoxHeader}
      {modalBody}
      {modalBoxFooter}
    </ModalBox>
  );
  return (
    <Backdrop>
      <FocusTrap
        active={!disableFocusTrap}
        focusTrapOptions={{ clickOutsideDeactivates: true, tabbableOptions: { displayCheck: 'none' } }}
        className={css(bullsEyeStyles.bullseye)}
      >
        {modalBox}
      </FocusTrap>
    </Backdrop>
  );
};
ModalContent.displayName = 'ModalContent';
