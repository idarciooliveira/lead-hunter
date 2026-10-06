import { describe, expect, it, vi } from "vitest";
import { downloadCsv, leadsToCsv } from "./csv";
import { LEADS } from "./fixtures";

describe("leads csv", () => {
	it("starts with a BOM and the backend header, one CRLF row per lead", () => {
		const csv = leadsToCsv(LEADS.slice(0, 2));
		expect(csv.charCodeAt(0)).toBe(0xfeff);
		expect(
			csv.slice(1).startsWith("id,campanha,nome,categoria,telefone,whatsapp,pontuacao,estado,pitch,motivos\r\n"),
		).toBe(true);
		expect(csv.endsWith("\r\n")).toBe(true);
		expect(csv.split("\r\n")).toHaveLength(4);
	});

	it("quotes fields with commas and doubles quotes", () => {
		const csv = leadsToCsv([
			{ ...LEADS[0], name: 'Padaria "Pão, Quente"', category: "Padaria", pitch: "", phone: null },
		]);
		expect(csv).toContain('"Padaria ""Pão, Quente"""');
	});

	it("prefixes free text that Excel would run as a formula, but never phones", () => {
		const csv = leadsToCsv([{ ...LEADS[0], name: "=HYPERLINK(1)", phone: "+244923111222", pitch: "" }]);
		expect(csv).toContain("'=HYPERLINK(1)");
		expect(csv).toContain("+244923111222,");
		expect(csv).not.toContain("'+244923111222");
	});

	it("joins the score reasons with semicolons and links WhatsApp with the pitch", () => {
		const [top] = LEADS;
		const csv = leadsToCsv([top]);
		const reasons = [...top.breakdown.stage1, ...(top.breakdown.stage2 ?? [])].map((l) => l.reason).join("; ");
		expect(csv).toContain(reasons);
		expect(csv).toContain("https://wa.me/244");
	});

	it("downloads the file in the browser", () => {
		const anchor = document.createElement("a");
		const click = vi.spyOn(anchor, "click").mockImplementation(() => {});
		vi.spyOn(document, "createElement").mockReturnValueOnce(anchor);
		vi.spyOn(URL, "createObjectURL").mockReturnValueOnce("blob:csv");
		const revoke = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
		downloadCsv("leads.csv", "a,b\r\n");
		expect(anchor.download).toBe("leads.csv");
		expect(click).toHaveBeenCalledOnce();
		expect(revoke).toHaveBeenCalledWith("blob:csv");
	});
});
