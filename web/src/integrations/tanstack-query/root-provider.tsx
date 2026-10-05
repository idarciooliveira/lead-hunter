import { QueryClient } from "@tanstack/react-query";

export function getContext() {
	// Loaders fill the cache on the server; a short stale time stops the client refetching it on hydration.
	const queryClient = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000 } } });
	return { queryClient };
}
