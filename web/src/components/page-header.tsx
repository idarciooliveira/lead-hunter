import type * as React from "react";
import { cn } from "#/lib/utils";

export function PageHeader({
	title,
	subtitle,
	actions,
	before,
	className,
}: {
	title: React.ReactNode;
	subtitle?: React.ReactNode;
	actions?: React.ReactNode;
	/** Rendered above the title, e.g. a back button. */
	before?: React.ReactNode;
	className?: string;
}) {
	return (
		<div className={cn("flex flex-wrap items-start justify-between gap-4", className)}>
			<div>
				{before && <div className="mb-2">{before}</div>}
				{typeof title === "string" ? <h1 className="m-0 text-xl font-semibold tracking-[-0.01em]">{title}</h1> : title}
				{subtitle && <div className="mt-0.5 text-mute">{subtitle}</div>}
			</div>
			{actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
		</div>
	);
}

export function Page({ children }: { children: React.ReactNode }) {
	return <div className="flex flex-col gap-4">{children}</div>;
}
