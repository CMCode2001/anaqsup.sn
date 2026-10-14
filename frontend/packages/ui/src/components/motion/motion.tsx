"use client";

import { LazyMotion, MotionConfig } from "motion/react";
// Point d'entrée dédié : évite que le barrel `motion/react` n'embarque tout le moteur.
import * as m from "motion/react-m";
import type { ReactNode } from "react";

const loadFeatures = () => import("./features").then((module) => module.default);

/**
 * Motion (ex-Framer Motion) en mode « léger » :
 * - `LazyMotion` + composants `m` : ~5 Ko au lieu de ~34 Ko pour `motion.div` ;
 *   les fonctionnalités d'animation sont téléchargées après l'hydratation ;
 * - `strict` : interdit `motion.*` sous ce provider (garde-fou du budget de performance) ;
 * - `reducedMotion="user"` : respecte le réglage système « réduire les animations ».
 */
export function MotionProvider({ children }: { children: ReactNode }) {
  return (
    <LazyMotion features={loadFeatures} strict>
      <MotionConfig reducedMotion="user">{children}</MotionConfig>
    </LazyMotion>
  );
}

/**
 * Apparition au défilement (fondu + léger zoom avant). À réserver au contenu SOUS la ligne de flottaison : le contenu
 * est masqué jusqu'au chargement du moteur (le <noscript> du layout le rend visible sans JS).
 */
export function Reveal({ children, delay = 0, className }: { children: ReactNode; delay?: number; className?: string }) {
  return (
    <m.div
      data-reveal=""
      className={className}
      initial={{ opacity: 0, y: 12, scale: 0.96 }}
      whileInView={{ opacity: 1, y: 0, scale: 1 }}
      viewport={{ once: true, margin: "0px 0px -10% 0px" }}
      transition={{ duration: 0.5, delay, ease: [0.22, 1, 0.36, 1] }}
    >
      {children}
    </m.div>
  );
}

/** Transition d'entrée d'un message d'état (rejouée à chaque changement de `stateKey`). */
export function FadeIn({ stateKey, children, className }: { stateKey: string; children: ReactNode; className?: string }) {
  return (
    <m.div
      key={stateKey}
      className={className}
      initial={{ opacity: 0, y: 4 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
    >
      {children}
    </m.div>
  );
}
