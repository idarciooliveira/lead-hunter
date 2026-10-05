import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Button } from "./button";
import { PlannedChip } from "./chip";
import { MultiSelect } from "./multi-select";
import { ProgressBar } from "./progress";
import { Segmented } from "./segmented";

describe("design system", () => {
	it("clamps the progress bar and reports its value", () => {
		render(<ProgressBar value={140} label="Orçamento" />);
		expect(screen.getByRole("progressbar", { name: "Orçamento" })).toHaveAttribute("aria-valuenow", "100");
	});

	it("marks planned steps", () => {
		render(<PlannedChip step={4} />);
		expect(screen.getByText("Planeado, passo 4")).toBeInTheDocument();
	});

	it("styles the primary button with the accent", () => {
		render(<Button variant="primary">Executar</Button>);
		expect(screen.getByRole("button", { name: "Executar" })).toHaveClass("bg-acc");
	});

	it("selects one segment at a time", async () => {
		const onChange = vi.fn();
		render(
			<Segmented
				label="Modo"
				value="a"
				onChange={onChange}
				options={[
					{ value: "a", label: "Guiado" },
					{ value: "b", label: "Importar YAML", count: 2 },
				]}
			/>,
		);
		expect(screen.getByRole("button", { name: "Guiado" })).toHaveAttribute("aria-pressed", "true");
		await userEvent.click(screen.getByRole("button", { name: /Importar YAML/ }));
		expect(onChange).toHaveBeenCalledWith("b");
	});
});

describe("multi select", () => {
	it("ticks and unticks several options", async () => {
		const onChange = vi.fn();
		render(
			<MultiSelect
				options={["A", "B"]}
				value={["A"]}
				onChange={onChange}
				placeholder="Escolher"
				searchPlaceholder="Procurar"
				emptyLabel="Nada"
			/>,
		);
		await userEvent.click(screen.getByRole("combobox"));
		await userEvent.click(await screen.findByRole("option", { name: "B" }));
		expect(onChange).toHaveBeenCalledWith(["A", "B"]);
		await userEvent.click(screen.getByRole("option", { name: "A" }));
		expect(onChange).toHaveBeenCalledWith([]);
	});

	it("adds a typed value with Enter when creation is allowed", async () => {
		const onChange = vi.fn();
		render(
			<MultiSelect
				options={["A"]}
				value={["A"]}
				onChange={onChange}
				placeholder="Escolher"
				searchPlaceholder="Procurar"
				emptyLabel="Nada"
				createLabel={(v) => `Adicionar "${v}"`}
			/>,
		);
		await userEvent.click(screen.getByRole("combobox"));
		await userEvent.type(screen.getByLabelText("Procurar"), "Zango Norte{Enter}");
		expect(onChange).toHaveBeenCalledWith(["A", "Zango Norte"]);
	});
});
