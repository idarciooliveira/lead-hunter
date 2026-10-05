import type * as React from "react";
import { cn } from "#/lib/utils";

export function Card({ className, ...props }: React.ComponentProps<"div">) {
	return <div data-slot="card" className={cn("rounded-lg border border-line bg-panel", className)} {...props} />;
}

/** Title row with a bottom rule, used by every titled card. */
export function CardHeader({
	title,
	aside,
	className,
}: {
	title: React.ReactNode;
	aside?: React.ReactNode;
	className?: string;
}) {
	return (
		<div className={cn("flex items-center justify-between gap-3 border-b border-line px-4 py-3", className)}>
			<h2 className="m-0 text-base font-semibold">{title}</h2>
			{aside}
		</div>
	);
}
