import {
  ActionGroup,
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertAttributeNameToForm, sortProviders } from "../../util";
import { FormFields } from "../ClientDetails";
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

  const [
    userInfoResponseEncryptionKeyManagementOpen,
    setUserInfoResponseEncryptionKeyManagementOpen,
  ] = useState(false);

  const [
    userInfoResponseEncryptionContentEncryptionOpen,
    setUserInfoResponseEncryptionContentEncryptionOpen,
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
            helpText={t("clients-help:accessTokenSignatureAlgorithm")}
            fieldLabelId="clients:accessTokenSignatureAlgorithm"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.access.token.signed.response.alg"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="accessTokenSignatureAlgorithm"
              variant={SelectVariant.single}
              onToggle={setAccessTokenOpen}
              isOpen={accessTokenOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setAccessTokenOpen(false);
              }}
              selections={field.value}
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
            helpText={t("clients-help:idTokenSignatureAlgorithm")}
            fieldLabelId="clients:idTokenSignatureAlgorithm"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.id.token.signed.response.alg"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="idTokenSignatureAlgorithm"
              variant={SelectVariant.single}
              onToggle={setIdTokenOpen}
              isOpen={idTokenOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setIdTokenOpen(false);
              }}
              selections={field.value}
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
            helpText={t("clients-help:idTokenEncryptionKeyManagementAlgorithm")}
            fieldLabelId="clients:idTokenEncryptionKeyManagementAlgorithm"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.id.token.encrypted.response.alg"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="idTokenEncryptionKeyManagementAlgorithm"
              variant={SelectVariant.single}
              onToggle={setIdTokenKeyManagementOpen}
              isOpen={idTokenKeyManagementOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setIdTokenKeyManagementOpen(false);
              }}
              selections={field.value}
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
            helpText={t(
              "clients-help:idTokenEncryptionContentEncryptionAlgorithm"
            )}
            fieldLabelId="clients:idTokenEncryptionContentEncryptionAlgorithm"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.id.token.encrypted.response.enc"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="idTokenEncryptionContentEncryptionAlgorithm"
              variant={SelectVariant.single}
              onToggle={setIdTokenContentOpen}
              isOpen={idTokenContentOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setIdTokenContentOpen(false);
              }}
              selections={field.value}
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
            helpText={t("clients-help:userInfoSignedResponseAlgorithm")}
            fieldLabelId="clients:userInfoSignedResponseAlgorithm"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.user.info.response.signature.alg"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="userInfoSignedResponseAlgorithm"
              variant={SelectVariant.single}
              onToggle={setUserInfoSignedResponseOpen}
              isOpen={userInfoSignedResponseOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setUserInfoSignedResponseOpen(false);
              }}
              selections={field.value}
            >
              {signatureOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("userInfoResponseEncryptionKeyManagementAlgorithm")}
        fieldId="userInfoResponseEncryptionKeyManagementAlgorithm"
        labelIcon={
          <HelpItem
            helpText={t(
              "clients-help:userInfoResponseEncryptionKeyManagementAlgorithm"
            )}
            fieldLabelId="clients:userInfoResponseEncryptionKeyManagementAlgorithm"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.user.info.encrypted.response.alg"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="userInfoResponseEncryptionKeyManagementAlgorithm"
              variant={SelectVariant.single}
              onToggle={setUserInfoResponseEncryptionKeyManagementOpen}
              isOpen={userInfoResponseEncryptionKeyManagementOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setUserInfoResponseEncryptionKeyManagementOpen(false);
              }}
              selections={field.value}
            >
              {cekManagementOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("userInfoResponseEncryptionContentEncryptionAlgorithm")}
        fieldId="userInfoResponseEncryptionContentEncryptionAlgorithm"
        labelIcon={
          <HelpItem
            helpText={t(
              "clients-help:userInfoResponseEncryptionContentEncryptionAlgorithm"
            )}
            fieldLabelId="clients:userInfoResponseEncryptionContentEncryptionAlgorithm"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.user.info.encrypted.response.enc"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="userInfoResponseEncryptionContentEncryptionAlgorithm"
              variant={SelectVariant.single}
              onToggle={setUserInfoResponseEncryptionContentEncryptionOpen}
              isOpen={userInfoResponseEncryptionContentEncryptionOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setUserInfoResponseEncryptionContentEncryptionOpen(false);
              }}
              selections={field.value}
            >
              {contentOptions}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("requestObjectSignatureAlgorithm")}
        fieldId="requestObjectSignatureAlgorithm"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:requestObjectSignatureAlgorithm")}
            fieldLabelId="clients:requestObjectSignatureAlgorithm"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.request.object.signature.alg"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="requestObjectSignatureAlgorithm"
              variant={SelectVariant.single}
              onToggle={setRequestObjectSignatureOpen}
              isOpen={requestObjectSignatureOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setRequestObjectSignatureOpen(false);
              }}
              selections={field.value}
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
            helpText={t("clients-help:requestObjectEncryption")}
            fieldLabelId="clients:requestObjectEncryption"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.request.object.encryption.alg"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="requestObjectEncryption"
              variant={SelectVariant.single}
              onToggle={setRequestObjectEncryptionOpen}
              isOpen={requestObjectEncryptionOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setRequestObjectEncryptionOpen(false);
              }}
              selections={field.value}
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
            helpText={t("clients-help:requestObjectEncoding")}
            fieldLabelId="clients:requestObjectEncoding"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.request.object.encryption.enc"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="requestObjectEncoding"
              variant={SelectVariant.single}
              onToggle={setRequestObjectEncodingOpen}
              isOpen={requestObjectEncodingOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setRequestObjectEncodingOpen(false);
              }}
              selections={field.value}
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
            helpText={t("clients-help:requestObjectRequired")}
            fieldLabelId="clients:requestObjectRequired"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.request.object.required"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="requestObjectRequired"
              variant={SelectVariant.single}
              onToggle={setRequestObjectRequiredOpen}
              isOpen={requestObjectRequiredOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setRequestObjectRequiredOpen(false);
              }}
              selections={field.value}
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
            helpText={t("clients-help:validRequestURIs")}
            fieldLabelId="clients:validRequestURIs"
          />
        }
      >
        <MultiLineInput
          name={convertAttributeNameToForm("attributes.request.uris")}
          aria-label={t("validRequestURIs")}
          addButtonLabel="clients:addRequestUri"
          stringify
        />
      </FormGroup>
      <FormGroup
        label={t("authorizationSignedResponseAlg")}
        fieldId="authorizationSignedResponseAlg"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:authorizationSignedResponseAlg")}
            fieldLabelId="clients:authorizationSignedResponseAlg"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.authorization.signed.response.alg"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="authorizationSignedResponseAlg"
              variant={SelectVariant.single}
              onToggle={setAuthorizationSignedOpen}
              isOpen={authorizationSignedOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setAuthorizationSignedOpen(false);
              }}
              selections={field.value}
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
            helpText={t("clients-help:authorizationEncryptedResponseAlg")}
            fieldLabelId="clients:authorizationEncryptedResponseAlg"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.authorization.encrypted.response.alg"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="authorizationEncryptedResponseAlg"
              variant={SelectVariant.single}
              onToggle={setAuthorizationEncryptedOpen}
              isOpen={authorizationEncryptedOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setAuthorizationEncryptedOpen(false);
              }}
              selections={field.value}
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
            helpText={t("clients-help:authorizationEncryptedResponseEnc")}
            fieldLabelId="clients:authorizationEncryptedResponseEnc"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.authorization.encrypted.response.enc"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="authorizationEncryptedResponseEnc"
              variant={SelectVariant.single}
              onToggle={setAuthorizationEncryptedResponseOpen}
              isOpen={authorizationEncryptedResponseOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setAuthorizationEncryptedResponseOpen(false);
              }}
              selections={field.value}
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
