import {
  Button,
  TextInputGroup,
  TextInputGroupMain,
  TextInputGroupUtilities,
} from "@patternfly/react-core";
import { ArrowRightIcon, SearchIcon, TimesIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

type SearchInputComponentProps = {
  value: string;
  onChange: (value: string) => void;
  onSearch: (value: string) => void;
  onClear: () => void;
  placeholder?: string;
  "aria-label"?: string;
};

export const SearchInputComponent = ({
  value,
  onChange,
  onSearch,
  onClear,
  placeholder,
  "aria-label": ariaLabel,
}: SearchInputComponentProps) => {
  const { t } = useTranslation();

  return (
    <>
      <TextInputGroup>
        <TextInputGroupMain
          icon={<SearchIcon />}
          value={value}
          onChange={(event: React.FormEvent<HTMLInputElement>) =>
            onChange(event.currentTarget.value)
          }
          placeholder={placeholder}
          aria-label={ariaLabel}
          data-testid="search-input"
        />
        <TextInputGroupUtilities style={{ marginInline: "0px" }}>
          {value && (
            <Button
              variant="plain"
              onClick={onClear}
              aria-label={t("clear")}
              data-testid="clear-search"
              icon={<TimesIcon />}
            />
          )}
        </TextInputGroupUtilities>
      </TextInputGroup>
      <Button
        icon={<ArrowRightIcon />}
        variant="control"
        style={{ marginLeft: "0.1rem" }}
        onClick={() => onSearch(value)}
        aria-label={t("search")}
        data-testid="search"
      />
    </>
  );
};
