import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  KeycloakSelect,
  label,
  SelectVariant,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Alert,
  AlertVariant,
  Button,
  ButtonVariant,
  Checkbox,
  InputGroup,
  InputGroupItem,
  SelectOption,
  Text,
  TextContent,
  TextInput,
  TextVariants,
} from "@patternfly/react-core";
import { CheckIcon } from "@patternfly/react-icons";
import { ReactNode, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Form } from "react-router-dom";
import { UserAttribute, UserFilter } from "./UserDataTable";

type UserDataTableAttributeSearchFormProps = {
  activeFilters: UserFilter;
  setActiveFilters: (filters: UserFilter) => void;
  profile: UserProfileConfig;
  createAttributeSearchChips: () => ReactNode;
  searchUserWithAttributes: () => void;
};

type UserFilterForm = UserAttribute & { exact: boolean };

export function UserDataTableAttributeSearchForm({
  activeFilters,
  setActiveFilters,
  profile,
  createAttributeSearchChips,
  searchUserWithAttributes,
}: UserDataTableAttributeSearchFormProps) {
  const { t } = useTranslation();
  const { addAlert } = useAlerts();
  const [selectAttributeKeyOpen, setSelectAttributeKeyOpen] = useState(false);

  const defaultValues: UserAttribute = {
    name: "",
    displayName: "",
    value: "",
  };

  const {
    getValues,
    register,
    reset,
    formState: { errors },
    setValue,
    setError,
    clearErrors,
  } = useForm<UserFilterForm>({
    mode: "onChange",
    defaultValues,
  });

  const isAttributeKeyDuplicate = () => {
    return activeFilters.userAttribute.some(
      (filter) => filter.name === getValues().name,
    );
  };

  const isAttributeNameValid = () => {
    let valid = false;
    if (!getValues().name.length) {
      setError("name", {
        type: "empty",
        message: t("searchUserByAttributeMissingKeyError"),
      });
    } else if (
      activeFilters.userAttribute.some(
        (filter) => filter.name === getValues().name,
      )
    ) {
      setError("name", {
        type: "conflict",
        message: t("searchUserByAttributeKeyAlreadyInUseError"),
      });
    } else {
      valid = true;
    }
    return valid;
  };

  const isAttributeValueValid = () => {
    let valid = false;
    if (!getValues().value.length) {
      setError("value", {
        type: "empty",
        message: t("searchUserByAttributeMissingValueError"),
      });
    } else {
      valid = true;
    }
    return valid;
  };

  const isAttributeValid = () =>
    isAttributeNameValid() && isAttributeValueValid();

  const addToFilter = () => {
    if (isAttributeValid()) {
      setActiveFilters({
        ...activeFilters,
        userAttribute: [...activeFilters.userAttribute, { ...getValues() }],
      });
      reset(defaultValues);
    } else {
      if (errors.name?.message) {
        addAlert(errors.name.message, AlertVariant.danger);
      }

      if (errors.value?.message) {
        addAlert(errors.value.message, AlertVariant.danger);
      }
    }
  };

  const clearActiveFilters = () => {
    const filtered = [...activeFilters.userAttribute].filter(
      (chip) => chip.name !== chip.name,
    );
    setActiveFilters({ ...activeFilters, userAttribute: filtered });
  };

  const createAttributeKeyInputField = () => {
    if (profile) {
      return (
        <KeycloakSelect
          data-testid="search-attribute-name-select"
          variant={SelectVariant.typeahead}
          onToggle={(isOpen) => setSelectAttributeKeyOpen(isOpen)}
          selections={getValues().displayName}
          onSelect={(selectedValue) => {
            setValue("displayName", selectedValue.toString());
            if (isAttributeKeyDuplicate()) {
              setError("name", { type: "conflict" });
            } else {
              clearErrors("name");
            }
          }}
          isOpen={selectAttributeKeyOpen}
          placeholderText={t("selectAttribute")}
          validated={errors.name && "error"}
          maxHeight={300}
          {...register("displayName", {
            required: true,
            validate: isAttributeNameValid,
          })}
        >
          {profile.attributes?.map((option) => (
            <SelectOption
              key={option.name}
              value={label(t, option.displayName!, option.name)}
              onClick={(e) => {
                e.stopPropagation();
                setSelectAttributeKeyOpen(false);
                setValue("name", option.name!);
              }}
            >
              {label(t, option.displayName!, option.name)}
            </SelectOption>
          ))}
        </KeycloakSelect>
      );
    } else {
      return (
        <TextInput
          id="name"
          placeholder={t("keyPlaceholder")}
          validated={errors.name && "error"}
          onKeyDown={(e) => e.key === "Enter" && addToFilter()}
          {...register("name", {
            required: true,
            validate: isAttributeNameValid,
          })}
        />
      );
    }
  };

  return (
    <Form
      className="user-attribute-search-form"
      data-testid="user-attribute-search-form"
    >
      <TextContent className="user-attribute-search-form-headline">
        <Text component={TextVariants.h2}>{t("selectAttributes")}</Text>
      </TextContent>
      <Alert
        isInline
        className="user-attribute-search-form-alert"
        variant="info"
        title={t("searchUserByAttributeDescription")}
        component="h3"
      />
      <TextContent className="user-attribute-search-form-key-value">
        <div className="user-attribute-search-form-left">
          <Text component={TextVariants.h3}>{t("key")}</Text>
        </div>
        <div className="user-attribute-search-form-right">
          <Text component={TextVariants.h3}>{t("value")}</Text>
        </div>
      </TextContent>
      <div className="user-attribute-search-form-left">
        {createAttributeKeyInputField()}
      </div>
      <div className="user-attribute-search-form-right">
        <InputGroup>
          <InputGroupItem>
            <TextInput
              id="value"
              placeholder={t("valuePlaceholder")}
              validated={errors.value && "error"}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addToFilter();
                }
              }}
              {...register("value", {
                required: true,
                validate: isAttributeValueValid,
              })}
            />
          </InputGroupItem>
          <InputGroupItem>
            <Button
              data-testid="user-attribute-search-add-filter-button"
              variant="control"
              icon={<CheckIcon />}
              onClick={addToFilter}
              aria-label={t("addToFilter")}
            />
          </InputGroupItem>
        </InputGroup>
      </div>
      {createAttributeSearchChips()}

      <div className="pf-v5-u-pt-lg">
        <Checkbox
          id="exact"
          data-testid="exact"
          label={t("exactSearch")}
          isChecked={activeFilters.exact}
          onChange={(_, value) => {
            setActiveFilters({
              ...activeFilters,
              exact: value,
            });
          }}
        />
      </div>
      <ActionGroup className="user-attribute-search-form-action-group">
        <Button
          data-testid="search-user-attribute-btn"
          variant="primary"
          type="submit"
          isDisabled={!activeFilters.userAttribute.length}
          onClick={searchUserWithAttributes}
        >
          {t("search")}
        </Button>
        <Button
          variant={ButtonVariant.link}
          onClick={() => {
            reset();
            clearActiveFilters();
          }}
        >
          {t("reset")}
        </Button>
      </ActionGroup>
    </Form>
  );
}
