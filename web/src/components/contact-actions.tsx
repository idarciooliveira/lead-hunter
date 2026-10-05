import { Phone } from "lucide-react";
import { Button, type ButtonProps } from "#/components/ui/button";
import { Kbd } from "#/components/ui/kbd";
import { WhatsAppIcon } from "#/components/whatsapp-icon";
import { telUrl, whatsappUrl } from "#/lib/format";

type Size = ButtonProps["size"];

/** Opens WhatsApp with the suggested message. Disabled when the lead has no phone. */
export function WhatsAppLink({
	phone,
	message,
	size = "sm",
	iconOnly,
	shortcut,
	className,
	children = "WhatsApp",
}: {
	phone: string | null;
	message: string;
	size?: Size;
	iconOnly?: boolean;
	shortcut?: string;
	className?: string;
	children?: React.ReactNode;
}) {
	return (
		<Button asChild variant="wa" size={size} className={className}>
			<a
				href={phone ? whatsappUrl(phone, message) : undefined}
				aria-disabled={phone ? undefined : true}
				target="_blank"
				rel="noopener"
				aria-label={iconOnly ? "Abrir WhatsApp" : undefined}
			>
				<WhatsAppIcon
					className={size === "touch" || size === "touch-icon" ? "size-[18px]" : "size-[13px]"}
					aria-hidden
				/>
				{!iconOnly && children}
				{shortcut && <Kbd onAccent>{shortcut}</Kbd>}
			</a>
		</Button>
	);
}

export function CallLink({ phone, size = "sm", label = true }: { phone: string | null; size?: Size; label?: boolean }) {
	return (
		<Button asChild size={size}>
			<a href={phone ? telUrl(phone) : undefined} aria-disabled={phone ? undefined : true} aria-label="Ligar">
				<Phone className={size === "touch" || size === "touch-icon" ? "size-[18px]" : "size-[13px]"} aria-hidden />
				{label && "Ligar"}
			</a>
		</Button>
	);
}
