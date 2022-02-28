export const isOUIAEnvironment = (): boolean => {
  try {
    return (
      (typeof window !== 'undefined' &&
        window.localStorage &&
        window.localStorage.getItem('ouia:enabled') &&
        window.localStorage['ouia:enabled'].toLowerCase() === 'true') ||
      false
    );
  } catch (exception) {
    return false;
  }
};
export const generateOUIAId = (): boolean =>
  (typeof window !== 'undefined' &&
    window.localStorage['ouia-generate-id'] &&
    window.localStorage['ouia-generate-id'].toLowerCase() === 'true') ||
  false;

let id = 0;
export const getUniqueId = (): number => id++;
