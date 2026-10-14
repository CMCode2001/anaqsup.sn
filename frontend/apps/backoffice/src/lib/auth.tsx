import type { AdminMe, AdminPermission } from "@anaqsup/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createContext, type ReactNode, use } from "react";

import { ApiError, api, unwrap } from "@/lib/api";
import { ChangePasswordPage } from "@/pages/ChangePasswordPage";
import { LoginPage } from "@/pages/LoginPage";
import { SplashScreen } from "@/components/SplashScreen";

const MeContext = createContext<AdminMe | null>(null);

export const meQueryKey = ["me"] as const;

/** Compte connecté : `null` si personne n'est connecté (401). */
export function useMeQuery() {
  return useQuery({
    queryKey: meQueryKey,
    queryFn: async () => {
      try {
        return await unwrap(api.GET("/admin/auth/me"));
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) return null;
        throw error;
      }
    },
    staleTime: 60_000,
    retry: false,
  });
}

/** Le compte connecté (à utiliser sous <AuthGate>). */
export function useMe(): AdminMe {
  const me = use(MeContext);
  if (!me) throw new Error("useMe() hors de <AuthGate>");
  return me;
}

export function useCan() {
  const me = useMe();
  return (permission: AdminPermission) => me.permissions.includes(permission);
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => unwrap(api.POST("/admin/auth/logout")),
    onSettled: () => {
      queryClient.clear();
      queryClient.setQueryData(meQueryKey, null);
    },
  });
}

/**
 * Porte d'entrée : écran de connexion si personne n'est connecté, changement obligatoire du mot de passe
 * provisoire, sinon l'application.
 */
export function AuthGate({ children }: { children: ReactNode }) {
  const me = useMeQuery();
  if (me.isPending) return <SplashScreen />;
  if (me.isError) return <SplashScreen error />;
  if (!me.data) return <LoginPage />;
  if (me.data.mustChangePassword) return <ChangePasswordPage forced me={me.data} />;
  return <MeContext value={me.data}>{children}</MeContext>;
}
