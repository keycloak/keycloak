import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import type { ClientForm } from "../ClientDetails";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { Toggle } from "./SamlConfig";

const SIGNATURE_ALGORITHMS = [
  "RSA_SHA1",
  "RSA_SHA256",
  "RSA_SHA256_MGF1",
  "RSA_SHA512",
  "RSA_SHA512_MGF1",
  "DSA_SHA1",
] as const;

const KEYNAME_TRANSFORMER = ["NONE", "KEY_ID", "CERT_SUBJECT"] as const;

const CANONICALIZATION = [
  { name: "EXCLUSIVE", value: "http://www.w3.org/2001/10/xml-exc-c14n#" },
  {
    name: "EXCLUSIVE_WITH_COMMENTS",
    value: "http://www.w3.org/2001/10/xml-exc-c14n#WithComments",
  },
  {
    name: "INCLUSIVE",
    value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
  },
  {
    name: "INCLUSIVE_WITH_COMMENTS",
    value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments",
  },
] as const;

export const SamlSignature = () => {
  const { t } = useTranslation("clients");
  const [algOpen, setAlgOpen] = useState(false);
  const [keyOpen, setKeyOpen] = useState(false);
  const [canOpen, setCanOpen] = useState(false);

  const { control, watch } = useFormContext<ClientForm>();

  const signDocs = watch("attributes.saml.server.signature");
  const signAssertion = watch("attributes.saml.assertion.signature");

  return (
    <FormAccess
      isHorizontal
      role="manage-clients"
      className="keycloak__capability-config__form"
    >
      <Toggle name="attributes.saml.server.signature" label="signDocuments" />
      <Toggle
        name="attributes.saml.assertion.signature"
        label="signAssertions"
      />
      {(signDocs === "true" || signAssertion === "true") && (
        <>
          <FormGroup
            label={t("signatureAlgorithm")}
            fieldId="signatureAlgorithm"
            labelIcon={
              <HelpItem
                helpText="clients-help:signatureAlgorithm"
                fieldLabelId="clients:signatureAlgorithm"
              />
            }
          >
            <Controller
              name="attributes.saml.signature.algorithm"
              defaultValue={SIGNATURE_ALGORITHMS[0]}
              Key
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="signatureAlgorithm"
                  onToggle={setAlgOpen}
                  onSelect={(_, value) => {
                    onChange(value.toString());
                    setAlgOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  aria-label={t("signatureAlgorithm")}
                  isOpen={algOpen}
                >
                  {SIGNATURE_ALGORITHMS.map((algorithm) => (
                    <SelectOption
                      selected={algorithm === value}
                      key={algorithm}
                      value={algorithm}
                    />
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("signatureKeyName")}
            fieldId="signatureKeyName"
            labelIcon={
              <HelpItem
                helpText="clients-help:signatureKeyName"
                fieldLabelId="clients:signatureKeyName"
              />
            }
          >
            <Controller
              name="attributes.saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer"
              defaultValue={KEYNAME_TRANSFORMER[0]}
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="signatureKeyName"
                  onToggle={setKeyOpen}
                  onSelect={(_, value) => {
                    onChange(value.toString());
                    setKeyOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  aria-label={t("signatureKeyName")}
                  isOpen={keyOpen}
                >
                  {KEYNAME_TRANSFORMER.map((key) => (
                    <SelectOption
                      selected={key === value}
                      key={key}
                      value={key}
                    />
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("canonicalization")}
            fieldId="canonicalization"
            labelIcon={
              <HelpItem
                helpText="clients-help:canonicalization"
                fieldLabelId="clients:canonicalization"
              />
            }
          >
            <Controller
              name="attributes.saml_signature_canonicalization_method"
              defaultValue={CANONICALIZATION[0].value}
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="canonicalization"
                  onToggle={setCanOpen}
                  onSelect={(_, value) => {
                    onChange(value.toString());
                    setCanOpen(false);
                  }}
                  selections={
                    CANONICALIZATION.find((can) => can.value === value)?.name
                  }
                  variant={SelectVariant.single}
                  aria-label={t("canonicalization")}
                  isOpen={canOpen}
                >
                  {CANONICALIZATION.map((can) => (
                    <SelectOption
                      selected={can.value === value}
                      key={can.name}
                      value={can.value}
                    >
                      {can.name}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
        </>
      )}
    </FormAccess>
  );
};
