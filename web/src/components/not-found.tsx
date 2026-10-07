import { Link } from "@tanstack/react-router";
import { FoxState } from "#/components/states";
import { Button } from "#/components/ui/button";

export function NotFound() {
	return (
		<FoxState
			title="Página não encontrada"
			text="Este endereço não existe no Lead Hunter."
			action={
				<Button asChild variant="primary">
					<Link to="/hoje">Ir para Hoje</Link>
				</Button>
			}
		/>
	);
}
