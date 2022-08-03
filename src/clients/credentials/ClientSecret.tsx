import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import {
  Alert,
  Button,
  FormGroup,
  InputGroup,
  Split,
  SplitItem,
} from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { PasswordInput } from "../../components/password-input/PasswordInput";
import { CopyToClipboardButton } from "../scopes/CopyToClipboardButton";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import useFormatDate from "../../utils/useFormatDate";

export type ClientSecretProps = {
  client: ClientRepresentation;
  secret: string;
  toggle: () => void;
};

type SecretInputProps = Omit<ClientSecretProps, "client"> & {
  id: string;
  buttonLabel: string;
};

const SecretInput = ({ id, buttonLabel, secret, toggle }: SecretInputProps) => {
  const { t } = useTranslation("clients");
  const form = useFormContext<ClientRepresentation>();

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
          isDisabled={form.formState.isDirty}
          onClick={toggle}
        >
          {t(buttonLabel)}
        </Button>
      </SplitItem>
    </Split>
  );
};

const ExpireDateFormatter = ({ time }: { time: number }) => {
  const { t } = useTranslation("clients");
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
  const { t } = useTranslation("clients");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [secretRotated, setSecretRotated] = useState<string | undefined>(
    client.attributes?.["client.secret.rotated"]
  );
  const secretExpirationTime: number =
    client.attributes?.["client.secret.expiration.time"];
  const secretRotatedExpirationTime: number =
    client.attributes?.["client.secret.rotated.expiration.time"];

  const expired = (time: number) => new Date().getTime() >= time * 1000;

  const [toggleInvalidateConfirm, InvalidateConfirm] = useConfirmDialog({
    titleKey: "clients:invalidateRotatedSecret",
    messageKey: "clients:invalidateRotatedSecretExplain",
    continueButtonLabel: "common:confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.invalidateSecret({
          id: client.id!,
        });
        setSecretRotated(undefined);
        addAlert(t("invalidateRotatedSuccess"));
      } catch (error) {
        addError("clients:invalidateRotatedError", error);
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
