import { ImageResponse } from "next/og";

// Icône de l'écran d'accueil (iPhone, iPad) : le même drapeau que le favicon, en PNG (format exigé par iOS).
// Pas d'arrondi : iOS arrondit lui-même les icônes.
export const size = { width: 180, height: 180 };
export const contentType = "image/png";

const FLAG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 180 180">
  <rect width="60" height="180" fill="#109939"/>
  <rect x="60" width="60" height="180" fill="#FFE500"/>
  <rect x="120" width="60" height="180" fill="#DE2627"/>
  <polygon fill="#109939" points="90.00,61.90 97.07,83.67 119.96,83.67 101.44,97.12 108.52,118.88 90.00,105.43 71.48,118.88 78.56,97.12 60.04,83.67 82.93,83.67"/>
</svg>`;

export default function AppleIcon() {
  return new ImageResponse(
    <img alt="" width={180} height={180} src={`data:image/svg+xml;base64,${Buffer.from(FLAG).toString("base64")}`} />,
    size,
  );
}
