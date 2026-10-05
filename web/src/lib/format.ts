/** Dollar amounts as the CLI prints them: `$3.42`. */
export function usd(value: number): string {
	return `$${value.toFixed(2)}`;
}

/** An estimate shown on buttons that spend money: `≈ $0.64`. */
export function estimate(value: number): string {
	return `≈ ${usd(value)}`;
}

export function percent(part: number, whole: number): number {
	if (whole <= 0) return 0;
	return Math.min(100, Math.max(0, (part / whole) * 100));
}

export function digits(phone: string): string {
	return phone.replace(/\D/g, "");
}

export function whatsappUrl(phone: string, message: string): string {
	return `https://wa.me/${digits(phone)}?text=${encodeURIComponent(message)}`;
}

export function telUrl(phone: string): string {
	return `tel:+${digits(phone)}`;
}

export function rating(value: number): string {
	return value.toFixed(1);
}

/** The team works in Luanda, so dates show in West Africa Time whatever the browser says. */
const TIME_ZONE = "Africa/Luanda";

function parts(date: Date) {
	const p = new Intl.DateTimeFormat("pt-PT", {
		timeZone: TIME_ZONE,
		weekday: "long",
		day: "numeric",
		month: "long",
		year: "numeric",
		hour: "2-digit",
		minute: "2-digit",
		hourCycle: "h23",
	}).formatToParts(date);
	const get = (type: Intl.DateTimeFormatPartTypes) => p.find((x) => x.type === type)?.value ?? "";
	return {
		weekday: get("weekday"),
		day: get("day"),
		month: get("month"),
		year: get("year"),
		time: `${get("hour")}:${get("minute")}`,
	};
}

function dayKey(date: Date): string {
	const p = parts(date);
	return `${p.year}-${p.month}-${p.day}`;
}

/** `hoje 10:02`, `ontem 17:12`, or `28 set 10:12`. */
export function when(iso: string, now: Date = new Date()): string {
	const date = new Date(iso);
	const p = parts(date);
	if (dayKey(date) === dayKey(now)) return `hoje ${p.time}`;
	if (dayKey(date) === dayKey(new Date(now.getTime() - 86_400_000))) return `ontem ${p.time}`;
	return `${p.day} ${p.month.slice(0, 3)} ${p.time}`;
}

/** `Segunda, 5 de outubro`. */
export function longDay(date: Date): string {
	const p = parts(date);
	const weekday = p.weekday.replace(/-feira$/, "");
	return `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)}, ${p.day} de ${p.month}`;
}

const MONTHS = [
	"Janeiro",
	"Fevereiro",
	"Março",
	"Abril",
	"Maio",
	"Junho",
	"Julho",
	"Agosto",
	"Setembro",
	"Outubro",
	"Novembro",
	"Dezembro",
];

/** `2026-10` → `Outubro 2026`. */
export function monthLabel(month: string): string {
	const [year, m] = month.split("-");
	return `${MONTHS[Number(m) - 1]} ${year}`;
}

/** `2026-10` → `Outubro`. */
export function monthName(month: string): string {
	return MONTHS[Number(month.split("-")[1]) - 1];
}
