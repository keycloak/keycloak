import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  Alert,
  Button,
  FormGroup,
  InputGroup,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { PasswordInput } from "ui-shared";
import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useAccess } from "../../context/access/Access";
import useFormatDate from "../../utils/useFormatDate";
import { CopyToClipboardButton } from "../scopes/CopyToClipboardButton";

export type ClientSecretProps = {
  client: ClientRepresentation;
  secret: string;
  toggle: () => void;
};

type SecretInputProps = ClientSecretProps & {
  id: string;
  buttonLabel: string;
};

const SecretInput = ({
  id,
  buttonLabel,
  client,
  secret,
  toggle,
}: SecretInputProps) => {
  const { t } = useTranslation();
  const form = useFormContext<ClientRepresentation>();
  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients") || client.access?.configure;

  return (
    <Split hasGutter>
      <SplitItem isFilled>
        <InputGroup>
          <PasswordInput id={id} value={secret} isReadOnly />
          <CopyToClipboardButton
            id={id}
            text={secret}
            label="clientSecret"
            variant="control"
          />
        </InputGroup>
      </SplitItem>
      <SplitItem>
        <Button
          variant="secondary"
          isDisabled={form.formState.isDirty || !isManager}
          onClick={toggle}
        >
          {t(buttonLabel)}
        </Button>
      </SplitItem>
    </Split>
  );
};

const ExpireDateFormatter = ({ time }: { time: number }) => {
  const { t } = useTranslation();
  const formatDate = useFormatDate();
  const unixTimeToString = (time: number) =>
    time
      ? t("secretExpiresOn", {
          time: formatDate(new Date(time * 1000), {
            dateStyle: "full",
            timeStyle: "long",
          }),
        })
      : undefined;

  return <div className="pf-u-my-md">{unixTimeToString(time)}</div>;
};

export const ClientSecret = ({ client, secret, toggle }: ClientSecretProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [secretRotated, setSecretRotated] = useState<string | undefined>(
    client.attributes?.["client.secret.rotated"],
  );
  const secretExpirationTime: number =
    client.attributes?.["client.secret.expiration.time"];
  const secretRotatedExpirationTime: number =
    client.attributes?.["client.secret.rotated.expiration.time"];

  const expired = (time: number) => new Date().getTime() >= time * 1000;

  const [toggleInvalidateConfirm, InvalidateConfirm] = useConfirmDialog({
    titleKey: "invalidateRotatedSecret",
    messageKey: "invalidateRotatedSecretExplain",
    continueButtonLabel: "confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.invalidateSecret({
          id: client.id!,
        });
        setSecretRotated(undefined);
        addAlert(t("invalidateRotatedSuccess"));
      } catch (error) {
        addError("invalidateRotatedError", error);
      }
    },
  });

  return (
    <>
      <InvalidateConfirm />
      <FormGroup
        label={t("clientSecret")}
        fieldId="kc-client-secret"
        className="pf-u-my-md"
      >
        <SecretInput
          id="kc-client-secret"
          client={client}
          secret={secret}
          toggle={toggle}
          buttonLabel="regenerate"
        />
        <ExpireDateFormatter time={secretExpirationTime} />
        {expired(secretExpirationTime) && (
          <Alert variant="warning" isInline title={t("secretHasExpired")} />
        )}
      </FormGroup>
      {secretRotated && (
        <FormGroup label={t("secretRotated")} fieldId="secretRotated">
          <SecretInput
            id="secretRotated"
            client={client}
            secret={secretRotated}
            toggle={toggleInvalidateConfirm}
            buttonLabel="invalidateSecret"
          />
          <ExpireDateFormatter time={secretRotatedExpirationTime} />
        </FormGroup>
      )}
    </>
  );
};
