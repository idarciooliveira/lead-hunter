import { Command as CommandPrimitive } from "cmdk";
import { Search } from "lucide-react";
import { Dialog as DialogPrimitive } from "radix-ui";
import type * as React from "react";
import { cn } from "#/lib/utils";
import { Kbd } from "./kbd";

/** The ⌘K palette shell: a dimmed page with a search card near the top. */
export function CommandDialog({
	open,
	onOpenChange,
	placeholder,
	children,
}: {
	open: boolean;
	onOpenChange: (open: boolean) => void;
	placeholder: string;
	children: React.ReactNode;
}) {
	return (
		<DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
			<DialogPrimitive.Portal>
				<DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-ovl" />
				<DialogPrimitive.Content
					aria-describedby={undefined}
					className="fixed top-[110px] left-1/2 z-50 w-[560px] max-w-[calc(100%-32px)] -translate-x-1/2 overflow-hidden rounded-lg border border-line bg-panel text-tx outline-none"
				>
					<DialogPrimitive.Title className="sr-only">Paleta de comandos</DialogPrimitive.Title>
					<CommandPrimitive loop>
						<div className="flex items-center gap-2 border-b border-line px-4 py-3">
							<Search className="size-[15px]" aria-hidden />
							<CommandPrimitive.Input
								placeholder={placeholder}
								aria-label="Paleta de comandos"
								className="h-7 flex-1 border-0 bg-transparent outline-0 placeholder:text-mute"
							/>
							<Kbd>Esc</Kbd>
						</div>
						<CommandPrimitive.List className="max-h-[360px] overflow-y-auto p-2">{children}</CommandPrimitive.List>
					</CommandPrimitive>
				</DialogPrimitive.Content>
			</DialogPrimitive.Portal>
		</DialogPrimitive.Root>
	);
}

export const CommandEmpty = ({ children }: { children: React.ReactNode }) => (
	<CommandPrimitive.Empty className="px-3 py-6 text-center text-mute">{children}</CommandPrimitive.Empty>
);

export function CommandItem({ className, ...props }: React.ComponentProps<typeof CommandPrimitive.Item>) {
	return (
		<CommandPrimitive.Item
			className={cn(
				"flex w-full cursor-pointer items-center gap-2 rounded-md px-3 py-2.5 text-left data-[selected=true]:bg-soft",
				className,
			)}
			{...props}
		/>
	);
}
