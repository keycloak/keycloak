export type ReplaceStringOptions = {
  skipFirst?: boolean;
};

export type ReplaceString<
  Input extends string,
  Search extends string,
  Replacement extends string,
  Options extends ReplaceStringOptions = object,
> = Input extends `${infer Head}${Search}${infer Tail}`
  ? Options["skipFirst"] extends true
    ? `${Head}${Search}${ReplaceString<Tail, Search, Replacement>}`
    : `${Head}${Replacement}${ReplaceString<Tail, Search, Replacement>}`
  : Input;
