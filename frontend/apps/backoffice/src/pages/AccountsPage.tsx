import type { AdminAccount, AdminRole } from "@anaqsup/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { KeyRoundIcon, Loader2Icon, PencilIcon, PlusIcon, Trash2Icon, UserPlusIcon } from "lucide-react";
import { type FormEvent, useState } from "react";
import { toast } from "@anaqsup/ui/components/sonner";

import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter,
  AlertDialogHeader, AlertDialogTitle,
} from "@anaqsup/ui/components/alert-dialog";
import { Badge } from "@anaqsup/ui/components/badge";
import { Button } from "@anaqsup/ui/components/button";
import { Card } from "@anaqsup/ui/components/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@anaqsup/ui/components/dialog";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@anaqsup/ui/components/select";
import { Skeleton } from "@anaqsup/ui/components/skeleton";
import { Switch } from "@anaqsup/ui/components/switch";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@anaqsup/ui/components/table";

import { PageTitle } from "@/components/PageTitle";
import { TemporaryPasswordField } from "@/components/TemporaryPasswordField";
import { api, dateTime, generatePassword, unwrap } from "@/lib/api";
import { useMe } from "@/lib/auth";

const accountsKey = ["accounts"] as const;
export const rolesKey = ["roles"] as const;

export function useRoles() {
  return useQuery({ queryKey: rolesKey, queryFn: () => unwrap(api.GET("/admin/roles")) });
}

function RoleSelect({ roles, value, onChange, disabled }: { roles: AdminRole[]; value: string; onChange: (id: string) => void; disabled?: boolean }) {
  return (
    <Select value={value} onValueChange={onChange} disabled={disabled}>
      <SelectTrigger id="role" className="w-full rounded-full"><SelectValue placeholder="Choisir un rôle" /></SelectTrigger>
      <SelectContent>
        {roles.map((role) => <SelectItem key={role.id} value={role.id}>{role.name}</SelectItem>)}
      </SelectContent>
    </Select>
  );
}

/** Création d'un compte : e-mail, nom, rôle et mot de passe provisoire. */
function CreateAccountDialog({ open, onOpenChange, roles }: { open: boolean; onOpenChange: (open: boolean) => void; roles: AdminRole[] }) {
  const queryClient = useQueryClient();
  const defaultRole = roles.find((r) => !r.system)?.id ?? roles[0]?.id ?? "";
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [roleId, setRoleId] = useState(defaultRole);
  const [temporaryPassword, setTemporaryPassword] = useState(generatePassword);
  const create = useMutation({
    mutationFn: () => unwrap(api.POST("/admin/accounts", { body: { email, displayName, roleId: roleId || defaultRole, temporaryPassword } })),
    onSuccess: (account) => {
      toast.success(`Compte créé pour ${account.displayName}.`, { description: "Transmettez-lui son mot de passe provisoire." });
      void queryClient.invalidateQueries({ queryKey: accountsKey });
      void queryClient.invalidateQueries({ queryKey: rolesKey });
      setEmail(""); setDisplayName(""); setTemporaryPassword(generatePassword());
      onOpenChange(false);
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); create.mutate(); };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <form onSubmit={submit} className="flex flex-col gap-5">
          <DialogHeader>
            <DialogTitle>Créer un compte</DialogTitle>
            <DialogDescription>La personne se connectera avec son e-mail et le mot de passe provisoire ci-dessous.</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="new-name">Nom complet</Label>
            <Input id="new-name" required value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="new-email">Adresse e-mail</Label>
            <Input id="new-email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="role">Rôle</Label>
            <RoleSelect roles={roles} value={roleId || defaultRole} onChange={setRoleId} />
          </div>
          <TemporaryPasswordField value={temporaryPassword} onChange={setTemporaryPassword} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Annuler</Button>
            <Button type="submit" disabled={create.isPending || !email || !displayName || temporaryPassword.length < 8}>
              {create.isPending ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <UserPlusIcon data-icon="inline-start" aria-hidden="true" />}
              Créer le compte
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

/** Modification : nom, rôle, activation. */
function EditAccountDialog({ account, roles, onClose }: { account: AdminAccount; roles: AdminRole[]; onClose: () => void }) {
  const me = useMe();
  const queryClient = useQueryClient();
  const self = me.id === account.id;
  const [displayName, setDisplayName] = useState(account.displayName);
  const [roleId, setRoleId] = useState(account.role.id);
  const [enabled, setEnabled] = useState(account.enabled);
  const update = useMutation({
    mutationFn: () => unwrap(api.PUT("/admin/accounts/{id}", { params: { path: { id: account.id } }, body: { displayName, roleId, enabled } })),
    onSuccess: () => {
      toast.success("Compte modifié.");
      void queryClient.invalidateQueries({ queryKey: accountsKey });
      void queryClient.invalidateQueries({ queryKey: rolesKey });
      onClose();
    },
    onError: (error) => toast.error(error.message),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); update.mutate(); };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <form onSubmit={submit} className="flex flex-col gap-5">
          <DialogHeader>
            <DialogTitle>Modifier le compte</DialogTitle>
            <DialogDescription>{account.email}</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="edit-name">Nom complet</Label>
            <Input id="edit-name" required value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="role">Rôle</Label>
            <RoleSelect roles={roles} value={roleId} onChange={setRoleId} disabled={self} />
            {self && <p className="text-xs text-muted-foreground">Vous ne pouvez pas changer votre propre rôle.</p>}
          </div>
          <div className="flex items-center justify-between gap-4 rounded-2xl border p-4">
            <div>
              <Label htmlFor="enabled">Compte actif</Label>
              <p className="text-xs text-muted-foreground">Un compte désactivé est déconnecté immédiatement.</p>
            </div>
            <Switch id="enabled" checked={enabled} onCheckedChange={setEnabled} disabled={self} />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Annuler</Button>
            <Button type="submit" disabled={update.isPending || !displayName}>
              {update.isPending && <Loader2Icon className="animate-spin" aria-hidden="true" />}Enregistrer
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

/** Nouveau mot de passe provisoire (mot de passe oublié). */
function ResetPasswordDialog({ account, onClose }: { account: AdminAccount; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [temporaryPassword, setTemporaryPassword] = useState(generatePassword);
  const reset = useMutation({
    mutationFn: () => unwrap(api.POST("/admin/accounts/{id}/password", { params: { path: { id: account.id } }, body: { temporaryPassword } })),
    onSuccess: () => {
      toast.success(`Nouveau mot de passe provisoire pour ${account.displayName}.`);
      void queryClient.invalidateQueries({ queryKey: accountsKey });
      onClose();
    },
    onError: (error) => toast.error(error.message),
  });
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <form onSubmit={(e) => { e.preventDefault(); reset.mutate(); }} className="flex flex-col gap-5">
          <DialogHeader>
            <DialogTitle>Nouveau mot de passe provisoire</DialogTitle>
            <DialogDescription>{account.displayName} devra le changer à sa prochaine connexion.</DialogDescription>
          </DialogHeader>
          <TemporaryPasswordField value={temporaryPassword} onChange={setTemporaryPassword} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Annuler</Button>
            <Button type="submit" disabled={reset.isPending || temporaryPassword.length < 8}>
              {reset.isPending ? <Loader2Icon className="animate-spin" aria-hidden="true" /> : <KeyRoundIcon data-icon="inline-start" aria-hidden="true" />}
              Attribuer
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function DeleteAccountDialog({ account, onClose }: { account: AdminAccount; onClose: () => void }) {
  const queryClient = useQueryClient();
  const remove = useMutation({
    mutationFn: () => unwrap(api.DELETE("/admin/accounts/{id}", { params: { path: { id: account.id } } })),
    onSuccess: () => {
      toast.success("Compte supprimé.");
      void queryClient.invalidateQueries({ queryKey: accountsKey });
      void queryClient.invalidateQueries({ queryKey: rolesKey });
      onClose();
    },
    onError: (error) => { toast.error(error.message); onClose(); },
  });
  return (
    <AlertDialog open onOpenChange={(open) => !open && onClose()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Supprimer le compte de {account.displayName} ?</AlertDialogTitle>
          <AlertDialogDescription>La personne ne pourra plus se connecter. Pour une absence temporaire, préférez « désactiver ».</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Annuler</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={(e) => { e.preventDefault(); remove.mutate(); }}>Supprimer</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

type Action = { kind: "edit" | "reset" | "delete"; account: AdminAccount } | null;

export function AccountsPage() {
  const me = useMe();
  const accounts = useQuery({ queryKey: accountsKey, queryFn: () => unwrap(api.GET("/admin/accounts")) });
  const roles = useRoles();
  const [creating, setCreating] = useState(false);
  const [action, setAction] = useState<Action>(null);

  return (
    <section className="space-y-6">
      <PageTitle
        title="Comptes"
        description="Les personnes qui peuvent se connecter à l'espace de gestion. Leurs droits dépendent de leur rôle."
        actions={<Button onClick={() => setCreating(true)} disabled={!roles.data}><PlusIcon data-icon="inline-start" aria-hidden="true" />Créer un compte</Button>}
      />

      <Card className="overflow-hidden py-0">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/60 hover:bg-muted/60">
              <TableHead className="pl-6">Nom</TableHead>
              <TableHead>Rôle</TableHead>
              <TableHead>État</TableHead>
              <TableHead>Dernière connexion</TableHead>
              <TableHead className="pr-6 text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {accounts.isPending && Array.from({ length: 3 }, (_, i) => (
              <TableRow key={i}><TableCell colSpan={5} className="px-6"><Skeleton className="h-8 w-full" /></TableCell></TableRow>
            ))}
            {accounts.data?.map((account) => (
              <TableRow key={account.id}>
                <TableCell className="pl-6">
                  <span className="block font-semibold">{account.displayName}{account.id === me.id && <span className="ml-2 text-xs font-normal text-muted-foreground">(vous)</span>}</span>
                  <span className="block text-xs text-muted-foreground">{account.email}</span>
                </TableCell>
                <TableCell><Badge variant="secondary" className="text-brand-ink">{account.role.name}</Badge></TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-1.5">
                    {account.enabled
                      ? <Badge className="bg-status-ok-surface text-status-ok">Actif</Badge>
                      : <Badge className="bg-status-neutral-surface text-status-neutral">Désactivé</Badge>}
                    {account.mustChangePassword && <Badge variant="outline">Mot de passe provisoire</Badge>}
                  </div>
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">{account.lastLoginAt ? dateTime.format(new Date(account.lastLoginAt)) : "Jamais"}</TableCell>
                <TableCell className="pr-6">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" aria-label={`Modifier ${account.displayName}`} onClick={() => setAction({ kind: "edit", account })}><PencilIcon aria-hidden="true" /></Button>
                    <Button variant="ghost" size="icon" aria-label={`Nouveau mot de passe provisoire pour ${account.displayName}`} onClick={() => setAction({ kind: "reset", account })}><KeyRoundIcon aria-hidden="true" /></Button>
                    <Button variant="ghost" size="icon" aria-label={`Supprimer ${account.displayName}`} disabled={account.id === me.id} onClick={() => setAction({ kind: "delete", account })}><Trash2Icon aria-hidden="true" /></Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      {roles.data && <CreateAccountDialog open={creating} onOpenChange={setCreating} roles={roles.data} />}
      {action?.kind === "edit" && roles.data && <EditAccountDialog account={action.account} roles={roles.data} onClose={() => setAction(null)} />}
      {action?.kind === "reset" && <ResetPasswordDialog account={action.account} onClose={() => setAction(null)} />}
      {action?.kind === "delete" && <DeleteAccountDialog account={action.account} onClose={() => setAction(null)} />}
    </section>
  );
}
