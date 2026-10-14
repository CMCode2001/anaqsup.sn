#!/usr/bin/env python3
"""Redirections 301 des URLs WordPress historiques (2 157 URLs indexées au 13/10/2026).

Deux étapes, avec revue humaine entre les deux :

  1. inventory : lit les sitemaps du site actuel et propose une cible par URL
       python3 -I devops/scripts/legacy-redirects.py inventory --out docs/migration/inventaire-urls-legacy.csv
  2. compile   : transforme les lignes AUTO/VALIDE en table Nginx, les lignes GONE en 410
       python3 -I devops/scripts/legacy-redirects.py compile --csv docs/migration/inventaire-urls-legacy.csv \
           --out devops/nginx/redirects

Statuts : AUTO (règle sûre), A_VALIDER (cible à confirmer par le référent données → passer à VALIDE),
VALIDE, GONE (contenu supprimé volontairement : 410).
"""
from __future__ import annotations

import argparse
import csv
import re
import sys
import urllib.parse
import urllib.request

SITEMAP = "https://anaqsup.sn/sitemap.xml"

# Pages institutionnelles : correspondance éditoriale explicite.
PAGES = {
    "/": ("/", "AUTO"),
    "/contact/": ("/contact", "AUTO"),
    "/missions/": ("/anaq-sup/missions", "AUTO"),
    "/organe-de-gouvernance/": ("/anaq-sup/gouvernance", "AUTO"),
    "/enseignement-superieur/": ("/anaq-sup", "A_VALIDER"),
    "/evaluation/": ("/procedures/accreditation", "A_VALIDER"),
    "/referentiels/": ("/ressources-documentaires/referentiels", "AUTO"),
    "/textes-reglementaires/": ("/ressources-documentaires/textes-reglementaires", "AUTO"),
    "/publications/": ("/ressources-documentaires/publications", "AUTO"),
    "/mediatheque/": ("/ressources-documentaires/publications", "AUTO"),
    "/wp-file-download-search/": ("/ressources-documentaires/publications", "AUTO"),
    "/actualites/": ("/actualites", "AUTO"),
    "/evenement/": ("/evenements", "AUTO"),
    "/habilitations/": ("/etablissements", "AUTO"),
    "/acreditations/": ("/formations", "AUTO"),
    "/accreditations-copy/": ("/formations", "AUTO"),
    "/accueil-english/": ("/", "A_VALIDER"),
    "/accueil-portugues/": ("/", "A_VALIDER"),
    "/accueil-العربية/": ("/", "A_VALIDER"),
    # Restes du thème et pages techniques : supprimés volontairement.
    "/company-overview/": (None, "GONE"),
    "/our-mission/": (None, "GONE"),
    "/test/": (None, "GONE"),
    "/login/": (None, "GONE"),
    "/register/": (None, "GONE"),
    "/edit-profile/": (None, "GONE"),
}


def locs(url: str) -> list[str]:
    with urllib.request.urlopen(url, timeout=30) as response:  # noqa: S310 (URL fixe, HTTPS)
        xml = response.read().decode("utf-8", errors="replace")
    xml = xml.replace("<![CDATA[", "").replace("]]>", "")
    return re.findall(r"<loc>([^<]+)</loc>", xml)


def propose(source: str, path: str) -> tuple[str | None, str, str]:
    """Renvoie (cible, statut, règle)."""
    if path in PAGES:
        target, status = PAGES[path]
        return target, status, "page"
    segments = [s for s in path.split("/") if s]
    if source.startswith("habilitation-sitemap") and len(segments) == 2:
        # Le slug définitif vient de la curation (table establishment_alias, kind ANCIEN_SLUG).
        return f"/etablissements/{segments[1]}", "A_VALIDER", "habilitation"
    if source.startswith("accreditations-sitemap") and len(segments) == 2:
        # Hétérogène (formations, établissements, rapports) : curation obligatoire.
        return f"/formations/{segments[1]}", "A_VALIDER", "accreditation"
    if source.startswith("category-sitemap"):
        return f"/actualites?categorie={segments[-1]}", "AUTO", "categorie"
    if source.startswith("post_tag-sitemap"):
        return "/actualites", "AUTO", "etiquette"
    if source.startswith("post-sitemap") and len(segments) == 1:
        # Les articles sont importés avec leur slug d'origine (news_item.legacy_wordpress_id).
        return f"/actualites/{segments[0]}", "AUTO", "article"
    if source.startswith("elementskit_template"):
        return None, "GONE", "gabarit"
    return "/", "A_VALIDER", "inconnu"


def inventory(args: argparse.Namespace) -> None:
    rows, seen = [], set()
    for sub in locs(args.sitemap):
        source = sub.rsplit("/", 1)[-1]
        for url in locs(sub):
            path = urllib.parse.unquote(urllib.parse.urlsplit(url).path) or "/"
            if path in seen:  # le sitemap WordPress liste certaines URLs plusieurs fois
                continue
            seen.add(path)
            target, status, rule = propose(source, path)
            rows.append({"ancienne_url": path, "cible": target or "", "statut": status, "regle": rule, "sitemap": source})
    with open(args.out, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["ancienne_url", "cible", "statut", "regle", "sitemap"])
        writer.writeheader()
        writer.writerows(sorted(rows, key=lambda r: (r["regle"], r["ancienne_url"])))
    by_status: dict[str, int] = {}
    for row in rows:
        by_status[row["statut"]] = by_status.get(row["statut"], 0) + 1
    print(f"{len(rows)} URLs → {args.out} | " + ", ".join(f"{k}: {v}" for k, v in sorted(by_status.items())))


def nginx_key(path: str) -> list[str]:
    # $uri est décodé : on écrit les clés décodées, avec et sans « / » final.
    variants = {path, path.rstrip("/") or "/"}
    return [f'"{v}"' for v in sorted(variants)]


def compile_maps(args: argparse.Namespace) -> None:
    # Nginx refuse une clé en double dans un `map` : on dédoublonne et on signale les conflits.
    redirects: dict[str, str] = {}
    gone: set[str] = set()
    conflicts, pending = [], 0
    with open(args.csv, encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            path, target, status = row["ancienne_url"], row["cible"], row["statut"]
            if status == "GONE":
                gone.update(nginx_key(path))
            elif status in ("AUTO", "VALIDE") and target and target.rstrip("/") != path.rstrip("/"):
                for key in nginx_key(path):
                    if redirects.setdefault(key, target) != target:
                        conflicts.append(f"{key} → {redirects[key]} | {target}")
            elif status == "A_VALIDER":
                pending += 1
    clash = gone & redirects.keys()
    if conflicts or clash:
        sys.exit("✗ Cibles contradictoires :\n  " + "\n  ".join(conflicts + [f"{k} : redirigée ET supprimée" for k in clash]))
    header = "# GÉNÉRÉ par devops/scripts/legacy-redirects.py — ne pas éditer à la main.\n"
    with open(f"{args.out}/legacy-wordpress.map", "w", encoding="utf-8") as handle:
        handle.write(header + "\n".join(f'{k} "{v}";' for k, v in sorted(redirects.items())) + "\n")
    with open(f"{args.out}/legacy-gone.map", "w", encoding="utf-8") as handle:
        handle.write(header + "\n".join(f"{k} 1;" for k in sorted(gone)) + "\n")
    print(f"{len(redirects)} clés de redirection, {len(gone)} clés 410, {pending} URLs encore À VALIDER")
    if args.strict and pending:
        sys.exit(f"✗ {pending} URLs sans cible validée (mode --strict, exigé avant la bascule DNS)")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    inv = sub.add_parser("inventory")
    inv.add_argument("--sitemap", default=SITEMAP)
    inv.add_argument("--out", required=True)
    inv.set_defaults(func=inventory)
    comp = sub.add_parser("compile")
    comp.add_argument("--csv", required=True)
    comp.add_argument("--out", required=True)
    comp.add_argument("--strict", action="store_true", help="échoue s'il reste des URLs À VALIDER")
    comp.set_defaults(func=compile_maps)
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
