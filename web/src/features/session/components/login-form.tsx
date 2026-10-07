import { useState } from "react";
import { FoxLogo } from "#/components/fox-logo";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Chip } from "#/components/ui/chip";
import { Input, Label } from "#/components/ui/field";
import { authClient } from "#/lib/auth-client";
import { safeNext } from "../next";

export function LoginForm({ next }: { next: string | undefined }) {
	const [error, setError] = useState<string | null>(null);
	const [pending, setPending] = useState(false);

	async function submit(event: React.FormEvent<HTMLFormElement>) {
		event.preventDefault();
		const form = new FormData(event.currentTarget);
		setPending(true);
		setError(null);
		const { error: failure } = await authClient.signIn.email({
			email: String(form.get("email")),
			password: String(form.get("password")),
		});
		if (failure) {
			setError(failure.message || "Não foi possível entrar.");
			setPending(false);
			return;
		}
		// A full load, so the server renders the page with the new session cookie.
		window.location.assign(safeNext(next));
	}

	return (
		<main className="flex min-h-screen items-center justify-center px-4">
			<Card className="w-full max-w-[360px] p-6">
				<div className="mb-5 flex items-center gap-2.5">
					<FoxLogo />
					<h1 className="m-0 text-base font-semibold tracking-[-0.01em]">Lead Hunter</h1>
				</div>
				<form onSubmit={submit} className="flex flex-col gap-3.5">
					<div>
						<Label htmlFor="email">Email</Label>
						<Input id="email" name="email" type="email" autoComplete="username" required autoFocus />
					</div>
					<div>
						<Label htmlFor="password">Palavra-passe</Label>
						<Input id="password" name="password" type="password" autoComplete="current-password" required />
					</div>
					{error && <Chip tone="bad">{error}</Chip>}
					<Button type="submit" variant="primary" disabled={pending}>
						{pending ? "A entrar…" : "Entrar"}
					</Button>
				</form>
			</Card>
		</main>
	);
}
