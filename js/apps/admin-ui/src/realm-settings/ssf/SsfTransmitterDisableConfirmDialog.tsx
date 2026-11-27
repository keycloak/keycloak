import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { ButtonVariant, Content, ModalVariant } from "@patternfly/react-core";
import { ReactElement } from "react";
import { useTranslation } from "react-i18next";

import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";

type UseSsfTransmitterDisableConfirmDialogProps = {
  onConfirm: () => void;
  onCancel: () => void;
};

/**
 * Drops queued SSF events (PENDING and HELD) for a realm via the SSF
 * admin REST API. Called from the realm-settings save flow before the
 * realm save persists {@code ssf.transmitterEnabled=false} — once the
 * flag is off, SsfAdminRealmResourceProviderFactory gates the SSF
 * admin paths and they return 404, so the cleanup must run while the
 * resource is still reachable.
 *
 * <p>Lives next to the confirm-dialog hook so the realm-settings tab
 * just imports a single SSF helper rather than re-implementing the
 * URL/auth/HTTP plumbing inline. Failures are propagated to the
 * caller; the realm-settings tab surfaces them as non-fatal toasts
 * and proceeds with the save (the {@code outbox-pending-max-age}
 * backstop will sweep any leftover PENDING rows on its own).
 */
export const deleteRealmSsfQueuedEvents = async (
  adminClient: KeycloakAdminClient,
): Promise<void> => {
  await adminClient.ssf.deleteQueuedEvents();
};

/**
 * Confirm dialog shown when an admin toggles the realm-level SSF
 * Transmitter feature off. Surfaces the cascading effects (silent
 * receiver pause, all SSF endpoints 404, queued events deleted on
 * save) so the off transition is a deliberate choice rather than an
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
      <Content>
        <Content component="p">
          {t("ssfTransmitterDisableConfirmIntro")}
        </Content>
        <Content component="ul">
          <Content component="li">
            {t("ssfTransmitterDisableConfirmBulletEndpoints")}
          </Content>
          <Content component="li">
            {t("ssfTransmitterDisableConfirmBulletEvents")}
          </Content>
          <Content component="li">
            {t("ssfTransmitterDisableConfirmBulletDelivery")}
          </Content>
          <Content component="li">
            {t("ssfTransmitterDisableConfirmBulletReceivers")}
          </Content>
        </Content>
        <Content component="p">
          {t("ssfTransmitterDisableConfirmRecommendation")}
        </Content>
      </Content>
    ),
  });
};
