import { getRequest } from "@tanstack/react-start/server";
import { getAuth, getPool } from "./auth.server";

/** Who is signed in and which organization their session works in. */
export type Viewer = { userId: string; name: string; email: string; orgId: string; orgName: string };

const viewers = new WeakMap<Request, Promise<Viewer | null>>();

/** The signed-in user for `request`, read once per request. Null without a valid session or organization. */
export function viewerOf(request: Request): Promise<Viewer | null> {
	let viewer = viewers.get(request);
	if (!viewer) {
		viewer = readViewer(request);
		viewers.set(request, viewer);
	}
	return viewer;
}

async function readViewer(request: Request): Promise<Viewer | null> {
	const session = await getAuth().api.getSession({ headers: request.headers });
	const orgId = (session?.session as { activeOrganizationId?: string | null } | undefined)?.activeOrganizationId;
	if (!session || !orgId) return null;
	const { rows } = await getPool().query<{ name: string }>("select name from organization where id = $1", [orgId]);
	if (rows.length === 0) return null;
	return { userId: session.user.id, name: session.user.name, email: session.user.email, orgId, orgName: rows[0].name };
}

/** The viewer of the request being served. Server functions and the API client call this, so no call goes out unsigned. */
export async function requireViewer(): Promise<Viewer> {
	const viewer = await viewerOf(getRequest());
	if (!viewer) throw new Error("not signed in");
	return viewer;
}
