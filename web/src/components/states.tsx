import type * as React from "react";
import { Chip } from "#/components/ui/chip";
import { Skeleton } from "#/components/ui/skeleton";
import { FoxLogo } from "./fox-logo";

/** Empty and error screens share the fox, a title, a line of help and one action. */
export function FoxState({
	title,
	text,
	error,
	action,
}: {
	title: string;
	text: string;
	error?: string;
	action?: React.ReactNode;
}) {
	return (
		<div className="flex flex-1 flex-col items-center justify-center gap-2.5 px-2 py-4 text-center">
			<FoxLogo size={64} />
			<div className="text-base font-semibold">{title}</div>
			<div className="max-w-[300px] text-mute">{text}</div>
			{error && <Chip tone="bad">{error}</Chip>}
			{action}
		</div>
	);
}

export function LoadingState({ text }: { text: string }) {
	return (
		<div className="flex flex-col gap-3" aria-busy="true">
			<div className="mt-1 flex flex-col gap-3">
				<Skeleton className="h-[18px] w-3/5" />
				<Skeleton className="h-11" />
				<Skeleton className="h-11" />
				<Skeleton className="h-11" />
				<Skeleton className="h-11 w-4/5" />
			</div>
			<div className="text-mute">{text}</div>
		</div>
	);
}
