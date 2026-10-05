import { describe, expect, it } from "vitest";
import { SAMPLE_ANSWERS, STEPS, yamlLines } from "./wizard";

const render = (lines: ReturnType<typeof yamlLines>) =>
	lines.map((l) => `${"  ".repeat(l.indent)}${l.key}${l.value}`).join("\n");

describe("campaign wizard", () => {
	it("asks eleven questions", () => {
		expect(STEPS).toHaveLength(11);
	});

	it("turns the answers into campaign YAML", () => {
		expect(render(yamlLines(SAMPLE_ANSWERS))).toBe(`name: "Clínicas Talatona"
sector: "Clínicas privadas"
problem: "Marcam consultas só por telefone e perdem pacientes pela demora."
service: "Sistema de marcações"
signals:
  qualify:
    - Sem marcação online
    - Queixas de demora nas reviews
  disqualify:
    - Cliente existente
min_reviews: 50
goal:
  qualified_leads: 100
  stop: goal_reached
search_terms:
  - clínica privada talatona
  - clínica kilamba
locations:
  - Talatona
  - Kilamba
limits:
  apify_usd: 1.50`);
	});

	it("writes empty lists inline and tags lines with their question", () => {
		const lines = yamlLines({ ...SAMPLE_ANSWERS, qualify: [], locations: [] });
		expect(render(lines)).toContain("  qualify: []");
		expect(render(lines)).toContain("locations: []");
		expect(lines.filter((l) => l.step === 8).map((l) => l.key.trim())).toEqual(["goal:", "qualified_leads:", "stop:"]);
	});
});
