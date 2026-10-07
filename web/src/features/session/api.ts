import { createServerFn } from "@tanstack/react-start";
import * as source from "./api.server";

/** Server function (ADR 0037): the shell shows who is signed in and which organization they work in. */
export const fetchViewer = createServerFn({ method: "GET" }).handler(() => source.fetchViewer());
