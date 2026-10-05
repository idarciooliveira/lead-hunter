import { Command as CommandPrimitive } from "cmdk";
import { Check, ChevronsUpDown, Plus } from "lucide-react";
import { Popover as PopoverPrimitive } from "radix-ui";
import { useState } from "react";
import { cn } from "#/lib/utils";
import { CommandEmpty, CommandItem } from "./command";

/** A searchable list where several options can be ticked. Built on Radix Popover and cmdk, as shadcn does. */
export function MultiSelect({
	id,
	options,
	value,
	onChange,
	placeholder,
	searchPlaceholder,
	emptyLabel,
	createLabel,
}: {
	id?: string;
	options: readonly string[];
	value: string[];
	onChange: (value: string[]) => void;
	placeholder: string;
	searchPlaceholder: string;
	emptyLabel: string;
	/** When set, typing a value that is not listed offers to add it. Enter picks it like any option. */
	createLabel?: (typed: string) => string;
}) {
	const [search, setSearch] = useState("");
	const toggle = (option: string) => {
		onChange(value.includes(option) ? value.filter((v) => v !== option) : [...value, option]);
		setSearch("");
	};
	// A saved value that is not in the catalogue stays visible and removable.
	const all = [...options, ...value.filter((v) => !options.includes(v))];
	const typed = search.trim();
	const canCreate = createLabel && typed !== "" && !all.some((o) => o.toLowerCase() === typed.toLowerCase());

	return (
		<PopoverPrimitive.Root>
			<PopoverPrimitive.Trigger
				id={id}
				role="combobox"
				className="flex min-h-8 w-full items-center justify-between gap-2 rounded-md border border-line2 bg-panel px-2.5 py-1 text-left focus:outline-2 focus:-outline-offset-1 focus:outline-acc"
			>
				<span className={cn("flex flex-wrap gap-x-2 gap-y-0.5", value.length === 0 && "text-mute")}>
					{value.length === 0 ? placeholder : value.map((v) => <span key={v}>{v}</span>)}
				</span>
				<ChevronsUpDown className="size-3.5 shrink-0 text-mute" aria-hidden />
			</PopoverPrimitive.Trigger>
			<PopoverPrimitive.Portal>
				<PopoverPrimitive.Content
					align="start"
					sideOffset={4}
					className="z-50 w-(--radix-popover-trigger-width) overflow-hidden rounded-lg border border-line bg-panel text-tx outline-none"
				>
					<CommandPrimitive loop>
						<CommandPrimitive.Input
							value={search}
							onValueChange={setSearch}
							placeholder={searchPlaceholder}
							aria-label={searchPlaceholder}
							className="h-9 w-full border-0 border-b border-line bg-transparent px-3 outline-0 placeholder:text-mute"
						/>
						<CommandPrimitive.List className="max-h-[240px] overflow-y-auto p-1">
							{canCreate ? (
								<CommandItem
									forceMount
									value={`__create__${typed}`}
									onSelect={() => toggle(typed)}
									className="px-2.5 py-1.5"
								>
									<Plus className="size-3.5" aria-hidden />
									{createLabel(typed)}
								</CommandItem>
							) : (
								<CommandEmpty>{emptyLabel}</CommandEmpty>
							)}
							{all.map((option) => (
								<CommandItem
									key={option}
									value={option}
									onSelect={() => toggle(option)}
									role="option"
									aria-selected={value.includes(option)}
									className="px-2.5 py-1.5"
								>
									<Check className={cn("size-3.5", !value.includes(option) && "invisible")} aria-hidden />
									{option}
								</CommandItem>
							))}
						</CommandPrimitive.List>
					</CommandPrimitive>
				</PopoverPrimitive.Content>
			</PopoverPrimitive.Portal>
		</PopoverPrimitive.Root>
	);
}
