import type { AdminPermission, AdminRole } from "@anaqsup/api-client";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckIcon, Loader2Icon, LockIcon, PencilIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { type FormEvent, useState } from "react";
import { toast } from "@anaqsup/ui/components/sonner";

import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter,
  AlertDialogHeader, AlertDialogTitle,
} from "@anaqsup/ui/components/alert-dialog";
import { Badge } from "@anaqsup/ui/components/badge";
import { Button } from "@anaqsup/ui/components/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@anaqsup/ui/components/card";
import { Checkbox } from "@anaqsup/ui/components/checkbox";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@anaqsup/ui/components/dialog";
import { Input } from "@anaqsup/ui/components/input";
import { Label } from "@anaqsup/ui/components/label";
import { Skeleton } from "@anaqsup/ui/components/skeleton";

import { PageTitle } from "@/components/PageTitle";
import { api, unwrap } from "@/lib/api";
import { PERMISSIONS } from "@/lib/permissions";
import { rolesKey, useRoles } from "@/pages/AccountsPage";

/** Création ou modification d'un rôle : nom et permissions cochées. */
function RoleDialog({ role, onClose }: { role?: AdminRole; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(role?.name ?? "");
  const [permissions, setPermissions] = useState<AdminPermission[]>(role?.permissions ?? []);
  const save = useMutation({
    mutationFn: () => {
      const body = { name, permissions };
      return role
        ? unwrap(api.PUT("/admin/roles/{id}", { params: { path: { id: role.id } }, body }))
        : unwrap(api.POST("/admin/roles", { body }));
    },
    onSuccess: (saved) => {
      toast.success(role ? `Rôle « ${saved.name} » modifié.` : `Rôle « ${saved.name} » créé.`);
      void queryClient.invalidateQueries({ queryKey: rolesKey });
      void queryClient.invalidateQueries({ queryKey: ["accounts"] });
      onClose();
    },
    onError: (error) => toast.error(error.message),
  });
  const toggle = (value: AdminPermission, checked: boolean) =>
    setPermissions((current) => (checked ? [...current, value] : current.filter((p) => p !== value)));
  const submit = (event: FormEvent) => { event.preventDefault(); save.mutate(); };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <form onSubmit={submit} className="flex flex-col gap-5">
          <DialogHeader>
            <DialogTitle>{role ? "Modifier le rôle" : "Créer un rôle"}</DialogTitle>
            <DialogDescription>Les comptes qui ont ce rôle n&apos;accèdent qu&apos;aux rubriques cochées.</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="role-name">Nom du rôle</Label>
            <Input id="role-name" required minLength={2} maxLength={60} value={name} onChange={(e) => setName(e.target.value)} placeholder="Ex. Chargé des évaluations" />
          </div>
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-sm font-medium">Permissions</legend>
            {PERMISSIONS.map((permission) => (
              <label key={permission.value} className="flex cursor-pointer items-start gap-3 rounded-2xl border p-3 hover:bg-muted/60 has-[[data-state=checked]]:border-primary has-[[data-state=checked]]:bg-muted/60">
                <Checkbox
                  checked={permissions.includes(permission.value)}
                  onCheckedChange={(checked) => toggle(permission.value, checked === true)}
                  className="mt-0.5"
                />
                <span>
                  <span className="block text-sm font-semibold">{permission.label}</span>
                  <span className="block text-xs text-muted-foreground">{permission.description}</span>
                </span>
              </label>
            ))}
          </fieldset>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Annuler</Button>
            <Button type="submit" disabled={save.isPending || name.trim().length < 2 || permissions.length === 0}>
              {save.isPending && <Loader2Icon className="animate-spin" aria-hidden="true" />}{role ? "Enregistrer" : "Créer le rôle"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function DeleteRoleDialog({ role, onClose }: { role: AdminRole; onClose: () => void }) {
  const queryClient = useQueryClient();
  const remove = useMutation({
    mutationFn: () => unwrap(api.DELETE("/admin/roles/{id}", { params: { path: { id: role.id } } })),
    onSuccess: () => { toast.success("Rôle supprimé."); void queryClient.invalidateQueries({ queryKey: rolesKey }); onClose(); },
    onError: (error) => { toast.error(error.message); onClose(); },
  });
  return (
    <AlertDialog open onOpenChange={(open) => !open && onClose()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Supprimer le rôle « {role.name} » ?</AlertDialogTitle>
          <AlertDialogDescription>Un rôle attribué à des comptes ne peut pas être supprimé.</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Annuler</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={(e) => { e.preventDefault(); remove.mutate(); }}>Supprimer</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

type Editing = { kind: "create" } | { kind: "edit" | "delete"; role: AdminRole } | null;

export function RolesPage() {
  const roles = useRoles();
  const [editing, setEditing] = useState<Editing>(null);

  return (
    <section className="space-y-6">
      <PageTitle
        title="Rôles et permissions"
        description="Un rôle regroupe des permissions. L'Administrateur a tous les droits ; vous pouvez créer d'autres rôles selon l'organisation de l'ANAQ-Sup."
        actions={<Button onClick={() => setEditing({ kind: "create" })}><PlusIcon data-icon="inline-start" aria-hidden="true" />Créer un rôle</Button>}
      />

      <ul className="grid gap-4 lg:grid-cols-2">
        {roles.isPending && Array.from({ length: 2 }, (_, i) => <li key={i}><Skeleton className="h-56 rounded-3xl" /></li>)}
        {roles.data?.map((role) => (
          <li key={role.id}>
            <Card className="h-full">
              <CardHeader>
                <CardTitle className="flex flex-wrap items-center gap-2 font-heading text-brand-ink">
                  {role.name}
                  {role.system && <Badge variant="outline" className="gap-1"><LockIcon aria-hidden="true" data-icon="inline-start" />Tous les droits</Badge>}
                </CardTitle>
                <CardDescription>{role.accountCount} compte{role.accountCount > 1 ? "s" : ""}</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-4">
                <ul className="grid gap-1.5 sm:grid-cols-2">
                  {PERMISSIONS.map((permission) => {
                    const granted = role.permissions.includes(permission.value);
                    return (
                      <li key={permission.value} className={granted ? "flex items-center gap-2 text-sm" : "flex items-center gap-2 text-sm text-muted-foreground/60 line-through"}>
                        <span className={granted ? "flex size-5 items-center justify-center rounded-full bg-primary text-primary-foreground" : "size-5 rounded-full border"}>
                          {granted && <CheckIcon className="size-3" aria-hidden="true" />}
                        </span>
                        {permission.label}
                        <span className="sr-only">{granted ? "(autorisé)" : "(non autorisé)"}</span>
                      </li>
                    );
                  })}
                </ul>
                {!role.system && (
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => setEditing({ kind: "edit", role })}><PencilIcon data-icon="inline-start" aria-hidden="true" />Modifier</Button>
                    <Button variant="ghost" size="sm" onClick={() => setEditing({ kind: "delete", role })} disabled={role.accountCount > 0}>
                      <Trash2Icon data-icon="inline-start" aria-hidden="true" />Supprimer
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>

      {editing?.kind === "create" && <RoleDialog onClose={() => setEditing(null)} />}
      {editing?.kind === "edit" && <RoleDialog role={editing.role} onClose={() => setEditing(null)} />}
      {editing?.kind === "delete" && <DeleteRoleDialog role={editing.role} onClose={() => setEditing(null)} />}
    </section>
  );
}
