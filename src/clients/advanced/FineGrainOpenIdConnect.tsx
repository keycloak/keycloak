import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Control, Controller } from "react-hook-form";
import {
  ActionGroup,
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { sortProviders } from "../../util";

type FineGrainOpenIdConnectProps = {
  control: Control<Record<string, any>>;
  save: () => void;
  reset: () => void;
};

export const FineGrainOpenIdConnect = ({
  control,
  save,
  reset,
}: FineGrainOpenIdConnectProps) => {
  const { t } = useTranslation("clients");
  const providers = useServerInfo().providers;
  const clientSignatureProviders = providers?.clientSignature.providers;
  const contentEncryptionProviders = providers?.contentencryption.providers;
  const cekManagementProviders = providers?.cekmanagement.providers;
  const signatureProviders = providers?.signature.providers;
  const [accessTokenOpen, setAccessTokenOpen] = useState(false);
  const [idTokenOpen, setIdTokenOpen] = useState(false);
  const [idTokenKeyManagementOpen, setIdTokenKeyManagementOpen] =
    useState(false);
  const [idTokenContentOpen, setIdTokenContentOpen] = useState(false);
  const [userInfoSignedResponseOpen, setUserInfoSignedResponseOpen] =
    useState(false);
  const [requestObjectSignatureOpen, setRequestObjectSignatureOpen] =
    useState(false);
  const [requestObjectRequiredOpen, setRequestObjectRequiredOpen] =
    useState(false);

  const keyOptions = [
    <SelectOption key="empty" value="">
      {t("common:choose")}
    </SelectOption>,
    ...sortProviders(clientSignatureProviders!).map((p) => (
      <SelectOption key={p} value={p} />
    )),
  ];
  const cekManagementOptions = [
    <SelectOption key="empty" value="">
      {t("common:choose")}
    </SelectOption>,
    ...sortProviders(cekManagementProviders!).map((p) => (
      <SelectOption key={p} value={p} />
    )),
  ];
  const signatureOptions = [
    <SelectOption key="unsigned" value="">
      {t("unsigned")}
    </SelectOption>,
    ...sortProviders(signatureProviders!).map((p) => (
      <SelectOption key={p} value={p} />
    )),
  ];
  const contentOptions = [
    <SelectOption key="empty" value="">
      {t("common:choose")}
    </SelectOption>,
    ...sortProviders(contentEncryptionProviders!).map((p) => (
      <SelectOption key={p} value={p} />
    )),
  ];

  const requestObjectOptions = [
    <SelectOption key="any" value="any">
      {t("common:any")}
    </SelectOption>,
    <SelectOption key="none" value="none">
      {t("common:none")}
    </SelectOption>,
    ...sortProviders(clientSignatureProviders!).map((p) => (
      <SelectOption key={p} value={p} />
    )),
  ];

  const requestObjectRequiredOptions = [
    "not required",
    "request or request_uri",
    "request only",
    "request_uri only",
  ].map((p) => (
    <SelectOption key={p} value={p}>
      {t(`requestObject.${p}`)}
    </SelectOption>
  ));

  return (
    <FormAccess role="manage-clients" isHorizontal>
      <FormGroup
        label={t("accessTokenSignatureAlgorithm")}
        fieldId="accessTokenSignatureAlgorithm"
        labelIcon={
          <HelpItem
            helpText="clients-help:accessTokenSignatureAlgorithm"
            fieldLabelId="clients:accessTokenSignatureAlgorithm"
          />
        }
      >
        <Controller
          name="attributes.access.token.signed.response.alg"
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="accessTokenSignatureAlgorithm"
              variant={SelectVariant.single}
              onToggle={setAccessTokenOpen}
              isOpen={accessTokenOpen}
              onSelect={(_, value) => {
                onChange(value);
                setAccessTokenOpen(false);
              }}
              selections={value}
            >
              {keyOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("idTokenSignatureAlgorithm")}
        fieldId="kc-id-token-signature"
        labelIcon={
          <HelpItem
            helpText="clients-help:idTokenSignatureAlgorithm"
            fieldLabelId="clients:idTokenSignatureAlgorithm"
          />
        }
      >
        <Controller
          name="attributes.id.token.signed.response.alg"
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="idTokenSignatureAlgorithm"
              variant={SelectVariant.single}
              onToggle={setIdTokenOpen}
              isOpen={idTokenOpen}
              onSelect={(_, value) => {
                onChange(value);
                setIdTokenOpen(false);
              }}
              selections={value}
            >
              {keyOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("idTokenEncryptionKeyManagementAlgorithm")}
        fieldId="idTokenEncryptionKeyManagementAlgorithm"
        labelIcon={
          <HelpItem
            helpText="clients-help:idTokenEncryptionKeyManagementAlgorithm"
            fieldLabelId="clients:idTokenEncryptionKeyManagementAlgorithm"
          />
        }
      >
        <Controller
          name="attributes.id.token.encrypted.response.alg"
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="idTokenEncryptionKeyManagementAlgorithm"
              variant={SelectVariant.single}
              onToggle={setIdTokenKeyManagementOpen}
              isOpen={idTokenKeyManagementOpen}
              onSelect={(_, value) => {
                onChange(value);
                setIdTokenKeyManagementOpen(false);
              }}
              selections={value}
            >
              {cekManagementOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("idTokenEncryptionContentEncryptionAlgorithm")}
        fieldId="idTokenEncryptionContentEncryptionAlgorithm"
        labelIcon={
          <HelpItem
            helpText="clients-help:idTokenEncryptionContentEncryptionAlgorithm"
            fieldLabelId="clients:idTokenEncryptionContentEncryptionAlgorithm"
          />
        }
      >
        <Controller
          name="attributes.id.token.encrypted.response.enc"
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="idTokenEncryptionContentEncryptionAlgorithm"
              variant={SelectVariant.single}
              onToggle={setIdTokenContentOpen}
              isOpen={idTokenContentOpen}
              onSelect={(_, value) => {
                onChange(value);
                setIdTokenContentOpen(false);
              }}
              selections={value}
            >
              {contentOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("userInfoSignedResponseAlgorithm")}
        fieldId="userInfoSignedResponseAlgorithm"
        labelIcon={
          <HelpItem
            helpText="clients-help:userInfoSignedResponseAlgorithm"
            fieldLabelId="clients:userInfoSignedResponseAlgorithm"
          />
        }
      >
        <Controller
          name="attributes.user.info.response.signature.alg"
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="userInfoSignedResponseAlgorithm"
              variant={SelectVariant.single}
              onToggle={setUserInfoSignedResponseOpen}
              isOpen={userInfoSignedResponseOpen}
              onSelect={(_, value) => {
                onChange(value);
                setUserInfoSignedResponseOpen(false);
              }}
              selections={value}
            >
              {signatureOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("requestObjectSignatureAlgorithm")}
        fieldId="requestObjectSignatureAlgorithm"
        labelIcon={
          <HelpItem
            helpText="clients-help:requestObjectSignatureAlgorithm"
            fieldLabelId="clients:requestObjectSignatureAlgorithm"
          />
        }
      >
        <Controller
          name="attributes.request_object_signature_alg"
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="requestObjectSignatureAlgorithm"
              variant={SelectVariant.single}
              onToggle={setRequestObjectSignatureOpen}
              isOpen={requestObjectSignatureOpen}
              onSelect={(_, value) => {
                onChange(value);
                setRequestObjectSignatureOpen(false);
              }}
              selections={value}
            >
              {requestObjectOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("requestObjectRequired")}
        fieldId="requestObjectRequired"
        labelIcon={
          <HelpItem
            helpText="clients-help:requestObjectRequired"
            fieldLabelId="clients:requestObjectRequired"
          />
        }
      >
        <Controller
          name="attributes.request.object.required"
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="requestObjectRequired"
              variant={SelectVariant.single}
              onToggle={setRequestObjectRequiredOpen}
              isOpen={requestObjectRequiredOpen}
              onSelect={(_, value) => {
                onChange(value);
                setRequestObjectRequiredOpen(false);
              }}
              selections={value}
            >
              {requestObjectRequiredOptions}
            </Select>
          )}
        />
      </FormGroup>
      <ActionGroup>
        <Button variant="secondary" id="fineGrainSave" onClick={save}>
          {t("common:save")}
        </Button>
        <Button id="fineGrainRevert" variant="link" onClick={reset}>
          {t("common:revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
