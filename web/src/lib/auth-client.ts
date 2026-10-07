import { createAuthClient } from "better-auth/react";

/** Talks to `/api/auth/*` on this origin. Sign-up stays off, so only sign-in and sign-out are used. */
export const authClient = createAuthClient();
