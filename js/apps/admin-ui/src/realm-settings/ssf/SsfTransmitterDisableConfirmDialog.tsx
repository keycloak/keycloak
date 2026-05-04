import {
  ButtonVariant,
  ModalVariant,
  Text,
  TextContent,
  TextList,
  TextListItem,
} from "@patternfly/react-core";
import { ReactElement } from "react";
import { useTranslation } from "react-i18next";

import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";

type UseSsfTransmitterDisableConfirmDialogProps = {
  onConfirm: () => void;
  onCancel: () => void;
};

/**
 * Confirm dialog shown when an admin toggles the realm-level SSF
 * Transmitter feature off. Surfaces the cascading effects (silent
 * receiver pause, queued events deferred and eventually dead-lettered
 * via the {@code outbox-pending-max-age} backstop, all SSF endpoints
 * 404) so the off transition is a deliberate choice rather than an
 * accidental flip.
 *
 * <p>Returns the same {@code [toggleDialog, Dialog]} tuple as
 * {@link useConfirmDialog} — the caller wires {@code toggleDialog}
 * into the realm-settings switch's onChange and renders {@code Dialog}
 * once in the surrounding tree.
 */
export const useSsfTransmitterDisableConfirmDialog = ({
  onConfirm,
  onCancel,
}: UseSsfTransmitterDisableConfirmDialogProps): [
  () => void,
  () => ReactElement,
] => {
  const { t } = useTranslation();

  return useConfirmDialog({
    titleKey: "ssfTransmitterDisableConfirmTitle",
    continueButtonLabel: "ssfTransmitterDisableConfirmContinue",
    continueButtonVariant: ButtonVariant.danger,
    variant: ModalVariant.medium,
    onConfirm,
    onCancel,
    children: (
      <TextContent>
        <Text>{t("ssfTransmitterDisableConfirmIntro")}</Text>
        <TextList>
          <TextListItem>
            {t("ssfTransmitterDisableConfirmBulletEndpoints")}
          </TextListItem>
          <TextListItem>
            {t("ssfTransmitterDisableConfirmBulletEvents")}
          </TextListItem>
          <TextListItem>
            {t("ssfTransmitterDisableConfirmBulletDelivery")}
          </TextListItem>
          <TextListItem>
            {t("ssfTransmitterDisableConfirmBulletReceivers")}
          </TextListItem>
        </TextList>
        <Text>{t("ssfTransmitterDisableConfirmRecommendation")}</Text>
        <Text>{t("ssfTransmitterDisableConfirmReenable")}</Text>
      </TextContent>
    ),
  });
};
