import { describe, expect, it } from "vitest";
import { estimate, longDay, monthLabel, percent, telUrl, usd, whatsappUrl, when } from "./format";

describe("format", () => {
	it("prints dollars the way the CLI does", () => {
		expect(usd(3.42)).toBe("$3.42");
		expect(usd(0)).toBe("$0.00");
		expect(estimate(0.64)).toBe("≈ $0.64");
	});

	it("clamps percentages and survives an empty whole", () => {
		expect(percent(5, 10)).toBe(50);
		expect(percent(15, 10)).toBe(100);
		expect(percent(1, 0)).toBe(0);
	});

	it("builds WhatsApp and phone links from formatted numbers", () => {
		expect(whatsappUrl("+244 923 410 872", "Olá, boa tarde!")).toBe(
			"https://wa.me/244923410872?text=Ol%C3%A1%2C%20boa%20tarde!",
		);
		expect(telUrl("+244 923 410 872")).toBe("tel:+244923410872");
	});

	it("shows run times relative to today in Luanda time", () => {
		const now = new Date("2026-10-05T12:00:00+01:00");
		expect(when("2026-10-05T10:02:00+01:00", now)).toBe("hoje 10:02");
		expect(when("2026-10-04T17:12:00+01:00", now)).toBe("ontem 17:12");
		expect(when("2026-09-28T10:12:00+01:00", now)).toBe("28 set 10:12");
	});

	it("names days and months in Portuguese", () => {
		expect(longDay(new Date("2026-10-05T12:00:00+01:00"))).toBe("Segunda, 5 de outubro");
		expect(monthLabel("2026-10")).toBe("Outubro 2026");
	});
});
