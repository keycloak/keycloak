import {
  FormErrorText,
  SelectControl,
  SwitchControl,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  FormFieldGroup,
  FormFieldGroupHeader,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import {
  MinusCircleIcon,
  PlusCircleIcon,
  TrashIcon,
} from "@patternfly/react-icons";
import { useEffect, useRef } from "react";
import { get, useFieldArray, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  CredentialSetForm,
  REQUESTED_CREDENTIALS,
} from "./requestedCredentials";

const CREDENTIAL_SETS = `${REQUESTED_CREDENTIALS}.credentialSets`;

type CredentialSetOptionProps = {
  setName: string;
  index: number;
  credentialIds: string[];
  onRemove?: () => void;
};

const CredentialSetOption = ({
  setName,
  index,
  credentialIds,
  onRemove,
}: CredentialSetOptionProps) => {
  const { t } = useTranslation();
  const name = `${setName}.options.${index}.credentials`;

  return (
    <Split hasGutter>
      <SplitItem isFilled>
        <SelectControl
          name={name}
          label={t("oid4vpCredentialSetOption", { index: index + 1 })}
          labelIcon={t("oid4vpCredentialSetOptionHelp")}
          variant="typeaheadMulti"
          controller={{
            defaultValue: [],
            rules: {
              required: { value: true, message: t("required") },
            },
          }}
          options={credentialIds}
        />
      </SplitItem>
      <SplitItem>
        <Button
          variant="plain"
          aria-label={t("oid4vpRemoveCredentialSetOption")}
          isDisabled={!onRemove}
          onClick={onRemove}
          data-testid={`${name}-remove`}
        >
          <MinusCircleIcon />
        </Button>
      </SplitItem>
    </Split>
  );
};

type CredentialSetFieldsProps = {
  index: number;
  credentialIds: string[];
  onRemove: () => void;
};

const CredentialSetFields = ({
  index,
  credentialIds,
  onRemove,
}: CredentialSetFieldsProps) => {
  const { t } = useTranslation();
  const name = `${CREDENTIAL_SETS}.${index}`;
  const { fields, append, remove } = useFieldArray({
    name: `${name}.options`,
    keyName: "key",
  });

  return (
    <FormFieldGroup
      header={
        <FormFieldGroupHeader
          titleText={{
            text: t("oid4vpCredentialSet", { index: index + 1 }),
            id: `oid4vp-credential-set-${index}`,
          }}
          actions={
            <Button
              variant="plain"
              aria-label={t("oid4vpRemoveCredentialSet")}
              onClick={onRemove}
              data-testid={`removeCredentialSet${index}`}
            >
              <TrashIcon />
            </Button>
          }
        />
      }
    >
      <SwitchControl
        name={`${name}.required`}
        label={t("oid4vpCredentialSetRequired")}
        labelIcon={t("oid4vpCredentialSetRequiredHelp")}
        labelOn={t("on")}
        labelOff={t("off")}
        defaultValue={true}
      />
      {fields.map((field, option) => (
        <CredentialSetOption
          key={field.key}
          setName={name}
          index={option}
          credentialIds={credentialIds}
          onRemove={fields.length > 1 ? () => remove(option) : undefined}
        />
      ))}
      <Button
        variant="link"
        icon={<PlusCircleIcon />}
        onClick={() => append({ credentials: [] })}
        data-testid={`addCredentialSetOption${index}`}
      >
        {t("oid4vpAddCredentialSetOption")}
      </Button>
    </FormFieldGroup>
  );
};

type CredentialSetsSettingsProps = {
  credentialIds: string[];
};

export const CredentialSetsSettings = ({
  credentialIds,
}: CredentialSetsSettingsProps) => {
  const { t } = useTranslation();
  const {
    trigger,
    formState: { errors },
  } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    name: CREDENTIAL_SETS,
    keyName: "key",
    rules: {
      validate: (sets) =>
        sets.length === 0 ||
        (sets as CredentialSetForm[]).some((set) => set.required) ||
        t("oid4vpNoRequiredCredentialSet"),
    },
  });
  const error = get(errors, `${CREDENTIAL_SETS}.root`);
  const sets: CredentialSetForm[] | undefined = useWatch({
    name: CREDENTIAL_SETS,
  });
  const anyRequired = (sets ?? []).some((set) => set.required);

  // Field array rules only run on submit and array actions, so a shown error is checked again here
  const checked = useRef(anyRequired);
  useEffect(() => {
    if (checked.current !== anyRequired) {
      checked.current = anyRequired;
      if (error) {
        void trigger(CREDENTIAL_SETS);
      }
    }
  }, [anyRequired, error, trigger]);

  return (
    <FormFieldGroup
      header={
        <FormFieldGroupHeader
          titleText={{
            text: t("oid4vpCredentialSets"),
            id: "oid4vp-credential-sets",
          }}
          titleDescription={t("oid4vpCredentialSetsHelp")}
        />
      }
    >
      {fields.map((field, index) => (
        <CredentialSetFields
          key={field.key}
          index={index}
          credentialIds={credentialIds}
          onRemove={() => remove(index)}
        />
      ))}
      {error && <FormErrorText message={error.message} />}
      <Button
        variant="link"
        icon={<PlusCircleIcon />}
        onClick={() =>
          append({ required: true, options: [{ credentials: [] }] })
        }
        data-testid="addCredentialSet"
      >
        {t("oid4vpAddCredentialSet")}
      </Button>
    </FormFieldGroup>
  );
};
