/** Profil renvoyé par GET /api/me. */
export interface Me {
  id: string;
  displayName: string;
  avatarUrl: string | null;
  role: 'USER' | 'ADMIN';
}

/** Tuile renvoyée par GET /api/modules. */
export interface PortalModule {
  key: string;
  label: string;
  description: string | null;
  icon: string | null;
  /** Adresse absolue du site du module, validée par le back. */
  url: string;
}
