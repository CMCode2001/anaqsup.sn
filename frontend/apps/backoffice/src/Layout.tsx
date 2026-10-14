import type { AdminPermission } from "@anaqsup/api-client";
import {
  BookOpenIcon, CalendarIcon, ChevronsUpDownIcon, HandshakeIcon, KeyRoundIcon, LandmarkIcon, LayoutDashboardIcon, LibraryIcon,
  LogOutIcon, NewspaperIcon, Settings2Icon, ShieldCheckIcon, UserCogIcon, UsersIcon,
} from "lucide-react";
import { NavLink, Outlet, useLocation } from "react-router";

import { BrandLogo } from "@anaqsup/ui/components/brand-logo";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger,
} from "@anaqsup/ui/components/dropdown-menu";
import { Separator } from "@anaqsup/ui/components/separator";
import {
  Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupContent, SidebarGroupLabel, SidebarHeader,
  SidebarInset, SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarProvider, SidebarTrigger,
} from "@anaqsup/ui/components/sidebar";

import { useCan, useLogout, useMe } from "@/lib/auth";

type Item = { to: string; label: string; Icon: typeof NewspaperIcon; permission?: AdminPermission };

// Rubriques du site (menu du portail) ; chacune n'apparaît qu'avec la permission correspondante.
const PUBLICATION: Item[] = [
  { to: "/actualites", label: "Actualités", Icon: NewspaperIcon, permission: "ACTUALITES" },
  { to: "/evenements", label: "Événements", Icon: CalendarIcon, permission: "EVENEMENTS" },
  { to: "/ressources", label: "Ressources documentaires", Icon: BookOpenIcon, permission: "DOCUMENTS" },
  { to: "/pages", label: "Pages institutionnelles", Icon: LibraryIcon, permission: "PAGES" },
  { to: "/avis", label: "Habilitations & accréditations", Icon: LandmarkIcon, permission: "AVIS" },
];

const ADMINISTRATION: Item[] = [
  { to: "/partenaires", label: "Partenaires", Icon: HandshakeIcon, permission: "PARTENAIRES" },
  { to: "/comptes", label: "Comptes", Icon: UsersIcon, permission: "COMPTES" },
  { to: "/parametres", label: "Paramètres du site", Icon: Settings2Icon, permission: "PARAMETRES" },
  { to: "/roles", label: "Rôles et permissions", Icon: ShieldCheckIcon, permission: "COMPTES" },
];

function NavGroup({ label, items }: { label: string; items: Item[] }) {
  const { pathname } = useLocation();
  const can = useCan();
  const visible = items.filter((item) => !item.permission || can(item.permission));
  if (visible.length === 0) return null;
  return (
    <SidebarGroup>
      <SidebarGroupLabel>{label}</SidebarGroupLabel>
      <SidebarGroupContent>
        <SidebarMenu>
          {visible.map(({ to, label: itemLabel, Icon }) => (
            <SidebarMenuItem key={to}>
              <SidebarMenuButton asChild isActive={pathname === to || pathname.startsWith(`${to}/`)} tooltip={itemLabel}>
                <NavLink to={to}>
                  <Icon aria-hidden="true" />
                  <span>{itemLabel}</span>
                </NavLink>
              </SidebarMenuButton>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}

function initials(name: string) {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join("");
}

function AccountMenu() {
  const me = useMe();
  const logout = useLogout();
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <SidebarMenuButton size="lg" className="data-[state=open]:bg-sidebar-accent">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-bold text-primary-foreground">
            {initials(me.displayName)}
          </span>
          <span className="grid flex-1 text-left text-sm leading-tight">
            <span className="truncate font-semibold">{me.displayName}</span>
            <span className="truncate text-xs opacity-70">{me.role.name}</span>
          </span>
          <ChevronsUpDownIcon className="ml-auto size-4" aria-hidden="true" />
        </SidebarMenuButton>
      </DropdownMenuTrigger>
      <DropdownMenuContent side="top" align="start" className="w-64">
        <DropdownMenuLabel className="font-normal">
          <span className="block font-semibold">{me.displayName}</span>
          <span className="block truncate text-xs text-muted-foreground">{me.email}</span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <NavLink to="/profil"><UserCogIcon aria-hidden="true" />Mon profil</NavLink>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <NavLink to="/profil#mot-de-passe"><KeyRoundIcon aria-hidden="true" />Changer mon mot de passe</NavLink>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={() => logout.mutate()}>
          <LogOutIcon aria-hidden="true" />Se déconnecter
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export function Layout() {
  const { pathname } = useLocation();
  return (
    <SidebarProvider>
      <Sidebar collapsible="icon">
        <SidebarHeader>
          {/* Charte : logotype uniquement sur fond blanc → pastille blanche ; masqué en mode replié (pas de variante). */}
          <div className="flex flex-col gap-2 px-1 py-1.5 group-data-[collapsible=icon]:hidden">
            <BrandLogo height={40} src="brand/logo-anaqsup.png" className="self-start rounded-xl p-2" />
            <span className="text-xs opacity-70">Espace de gestion</span>
          </div>
        </SidebarHeader>
        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupContent>
              <SidebarMenu>
                <SidebarMenuItem>
                  <SidebarMenuButton asChild isActive={pathname === "/"} tooltip="Tableau de bord">
                    <NavLink to="/" end><LayoutDashboardIcon aria-hidden="true" /><span>Tableau de bord</span></NavLink>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
          <NavGroup label="Publication" items={PUBLICATION} />
          <NavGroup label="Administration" items={ADMINISTRATION} />
        </SidebarContent>
        <SidebarFooter>
          <SidebarMenu>
            <SidebarMenuItem><AccountMenu /></SidebarMenuItem>
          </SidebarMenu>
        </SidebarFooter>
      </Sidebar>
      <SidebarInset>
        <header className="flex h-14 items-center gap-2 border-b bg-background px-4">
          <SidebarTrigger aria-label="Afficher ou masquer la navigation" />
          <Separator orientation="vertical" className="h-5" />
          <span className="text-sm text-muted-foreground">Espace de gestion · anaqsup.sn</span>
        </header>
        <main className="flex-1 bg-muted/40 p-4 md:p-8">
          <Outlet />
        </main>
      </SidebarInset>
    </SidebarProvider>
  );
}
