import { whatsappUrl } from "#/lib/format";
import type { Lead } from "./schema";

/**
 * The leads on screen as a CSV file, with the backend export's columns and
 * rules (ADR 0041): UTF-8 with a byte order mark, CRLF rows, quoted fields and
 * a leading apostrophe on free text that Excel would run as a formula. The
 * per-campaign `GET .../leads.csv` stays for scripts; the Hoje queue spans
 * every campaign, so the web builds its file from the leads it already shows.
 */
export function leadsToCsv(leads: Lead[]): string {
	const out = [`\uFEFF${HEADER.join(",")}`];
	for (const lead of leads) {
		out.push(
			[
				plain(lead.id),
				text(lead.campaignSlug),
				text(lead.name),
				text(lead.category),
				plain(lead.phone ?? ""),
				plain(lead.phone ? whatsappUrl(lead.phone, lead.pitch) : ""),
				plain(lead.score === null ? "" : String(lead.score)),
				plain(lead.status),
				text(lead.pitch),
				text([...lead.breakdown.stage1, ...(lead.breakdown.stage2 ?? [])].map((l) => l.reason).join("; ")),
			].join(","),
		);
	}
	return `${out.join("\r\n")}\r\n`;
}

/** Saves the CSV in the browser as a download. */
export function downloadCsv(filename: string, csv: string): void {
	const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
	const a = document.createElement("a");
	a.href = url;
	a.download = filename;
	document.body.appendChild(a);
	a.click();
	a.remove();
	URL.revokeObjectURL(url);
}

const HEADER = [
	"id",
	"campanha",
	"nome",
	"categoria",
	"telefone",
	"whatsapp",
	"pontuacao",
	"estado",
	"pitch",
	"motivos",
];

/** A value that is already safe: an id, a phone, a link, a score or a status. */
function plain(value: string): string {
	return quoteField(value);
}

/**
 * Free text from Maps or the LLM. A value starting with =, +, -, @, a tab or
 * a carriage return would run as a formula in Excel, so it gets a leading
 * apostrophe. Mirrors `LeadCsv.text` in the backend.
 */
function text(value: string): string {
	const formula = value !== "" && "=+-@\t\r".includes(value.charAt(0));
	return quoteField(formula ? `'${value}` : value);
}

function quoteField(value: string): string {
	const quote = value.includes(",") || value.includes('"') || value.includes("\n") || value.includes("\r");
	return quote ? `"${value.replace(/"/g, '""')}"` : value;
}
