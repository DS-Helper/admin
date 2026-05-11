export interface SelectOptionItem {
  value: string;
  label: string;
}

export interface SearchSelectConfig {
  id: string;
  placeholder: string;
  value: string;
  options: SelectOptionItem[];
  onChange: (value: string) => void;
}

export interface SearchSelectBarProps {
  searchPlaceholder: string;
  searchValue: string;
  onSearchChange: (value: string) => void;
  selects: SearchSelectConfig[];
  onSearchButtonClick?: () => void;
  searchButtonLabel?: string;
}
