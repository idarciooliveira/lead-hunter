import { cn } from "#/lib/utils";

export function Skeleton({ className }: { className?: string }) {
	return <div aria-hidden className={cn("animate-pulse-soft rounded-sm bg-soft", className)} />;
}
