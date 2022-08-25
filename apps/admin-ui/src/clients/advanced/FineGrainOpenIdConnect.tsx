import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
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
import { convertAttributeNameToForm, sortProviders } from "../../util";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { ApplicationUrls } from "./ApplicationUrls";

type FineGrainOpenIdConnectProps = {
  save: () => void;
  reset: () => void;
  hasConfigureAccess?: boolean;
};

export const FineGrainOpenIdConnect = ({
  save,
  reset,
  hasConfigureAccess,
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
  const [requestObjectEncryptionOpen, setRequestObjectEncryptionOpen] =
    useState(false);
  const [requestObjectEncodingOpen, setRequestObjectEncodingOpen] =
    useState(false);
  const [authorizationSignedOpen, setAuthorizationSignedOpen] = useState(false);
  const [authorizationEncryptedOpen, setAuthorizationEncryptedOpen] =
    useState(false);
  const [
    authorizationEncryptedResponseOpen,
    setAuthorizationEncryptedResponseOpen,
  ] = useState(false);

  const { control } = useFormContext();

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

  const requestObjectEncryptionOptions = [
    <SelectOption key="any" value="any">
      {t("common:any")}
    </SelectOption>,
    ...sortProviders(cekManagementProviders!).map((p) => (
      <SelectOption key={p} value={p} />
    )),
  ];

  const requestObjectEncodingOptions = [
    <SelectOption key="any" value="any">
      {t("common:any")}
    </SelectOption>,
    ...sortProviders(contentEncryptionProviders!).map((p) => (
      <SelectOption key={p} value={p} />
    )),
  ];

  const authorizationSignedResponseOptions = [
    <SelectOption key="empty" value="">
      {t("common:choose")}
    </SelectOption>,
    ...sortProviders(signatureProviders!).map((p) => (
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
    <FormAccess
      role="manage-clients"
      fineGrainedAccess={hasConfigureAccess}
      isHorizontal
    >
      <ApplicationUrls />
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
          name={convertAttributeNameToForm(
            "attributes.access.token.signed.response.alg"
          )}
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
          name={convertAttributeNameToForm(
            "attributes.id.token.signed.response.alg"
          )}
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
          name={convertAttributeNameToForm(
            "attributes.id.token.encrypted.response.alg"
          )}
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
          name={convertAttributeNameToForm(
            "attributes.id.token.encrypted.response.enc"
          )}
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
          name={convertAttributeNameToForm(
            "attributes.user.info.response.signature.alg"
          )}
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
          name={convertAttributeNameToForm(
            "attributes.request.object.signature.alg"
          )}
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
        label={t("requestObjectEncryption")}
        fieldId="requestObjectEncryption"
        labelIcon={
          <HelpItem
            helpText="clients-help:requestObjectEncryption"
            fieldLabelId="clients:requestObjectEncryption"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm(
            "attributes.request.object.encryption.alg"
          )}
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="requestObjectEncryption"
              variant={SelectVariant.single}
              onToggle={setRequestObjectEncryptionOpen}
              isOpen={requestObjectEncryptionOpen}
              onSelect={(_, value) => {
                onChange(value);
                setRequestObjectEncryptionOpen(false);
              }}
              selections={value}
            >
              {requestObjectEncryptionOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("requestObjectEncoding")}
        fieldId="requestObjectEncoding"
        labelIcon={
          <HelpItem
            helpText="clients-help:requestObjectEncoding"
            fieldLabelId="clients:requestObjectEncoding"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm(
            "attributes.request.object.encryption.enc"
          )}
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="requestObjectEncoding"
              variant={SelectVariant.single}
              onToggle={setRequestObjectEncodingOpen}
              isOpen={requestObjectEncodingOpen}
              onSelect={(_, value) => {
                onChange(value);
                setRequestObjectEncodingOpen(false);
              }}
              selections={value}
            >
              {requestObjectEncodingOptions}
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
          name={convertAttributeNameToForm(
            "attributes.request.object.required"
          )}
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
      <FormGroup
        label={t("validRequestURIs")}
        fieldId="validRequestURIs"
        labelIcon={
          <HelpItem
            helpText="clients-help:validRequestURIs"
            fieldLabelId="clients:validRequestURIs"
          />
        }
      >
        <MultiLineInput
          name={convertAttributeNameToForm("attributes.request.uris")}
          aria-label={t("validRequestURIs")}
          addButtonLabel="clients:addRequestUri"
        />
      </FormGroup>
      <FormGroup
        label={t("authorizationSignedResponseAlg")}
        fieldId="authorizationSignedResponseAlg"
        labelIcon={
          <HelpItem
            helpText="clients-help:authorizationSignedResponseAlg"
            fieldLabelId="clients:authorizationSignedResponseAlg"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm(
            "attributes.authorization.signed.response.alg"
          )}
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="authorizationSignedResponseAlg"
              variant={SelectVariant.single}
              onToggle={setAuthorizationSignedOpen}
              isOpen={authorizationSignedOpen}
              onSelect={(_, value) => {
                onChange(value);
                setAuthorizationSignedOpen(false);
              }}
              selections={value}
            >
              {authorizationSignedResponseOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("authorizationEncryptedResponseAlg")}
        fieldId="authorizationEncryptedResponseAlg"
        labelIcon={
          <HelpItem
            helpText="clients-help:authorizationEncryptedResponseAlg"
            fieldLabelId="clients:authorizationEncryptedResponseAlg"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm(
            "attributes.authorization.encrypted.response.alg"
          )}
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="authorizationEncryptedResponseAlg"
              variant={SelectVariant.single}
              onToggle={setAuthorizationEncryptedOpen}
              isOpen={authorizationEncryptedOpen}
              onSelect={(_, value) => {
                onChange(value);
                setAuthorizationEncryptedOpen(false);
              }}
              selections={value}
            >
              {cekManagementOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("authorizationEncryptedResponseEnc")}
        fieldId="authorizationEncryptedResponseEnc"
        labelIcon={
          <HelpItem
            helpText="clients-help:authorizationEncryptedResponseEnc"
            fieldLabelId="clients:authorizationEncryptedResponseEnc"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm(
            "attributes.authorization.encrypted.response.enc"
          )}
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="authorizationEncryptedResponseEnc"
              variant={SelectVariant.single}
              onToggle={setAuthorizationEncryptedResponseOpen}
              isOpen={authorizationEncryptedResponseOpen}
              onSelect={(_, value) => {
                onChange(value);
                setAuthorizationEncryptedResponseOpen(false);
              }}
              selections={value}
            >
              {contentOptions}
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
