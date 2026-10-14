import type en from "../../messages/en.json";
import type fr from "../../messages/fr.json";

/*
 * Garde-fou de compilation : fr.json et en.json doivent avoir exactement les mêmes clés.
 * Une traduction oubliée (ou une clé en trop) fait échouer `tsc`, donc la CI.
 */
type Keys<T> = T extends string
  ? never
  : { [K in keyof T & string]: T[K] extends string ? K : K | `${K}.${Keys<T[K]>}` }[keyof T & string];

type Aligned<A, B> = [Exclude<Keys<A>, Keys<B>>] extends [never] ? true : Exclude<Keys<A>, Keys<B>>;

export const missingInEnglish: Aligned<typeof fr, typeof en> = true;
export const missingInFrench: Aligned<typeof en, typeof fr> = true;
