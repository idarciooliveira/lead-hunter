import { authEnabled } from "#/lib/api-config.server";
import { requireViewer } from "#/lib/session.server";
import type { Viewer } from "./schema";

/** The signed-in user, or null when the app runs on fixtures. */
export async function fetchViewer(): Promise<Viewer | null> {
	if (!authEnabled()) return null;
	const { name, email, orgName } = await requireViewer();
	return { name, email, orgName };
}
