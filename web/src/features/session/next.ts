/** The page to open after signing in. Only a path on this site counts, so a crafted link cannot send people elsewhere. */
export function safeNext(next: string | undefined): string {
	if (next?.startsWith("/") && !next.startsWith("//") && !next.startsWith("/\\")) return next;
	return "/hoje";
}
