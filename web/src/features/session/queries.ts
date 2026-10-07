import { queryOptions } from "@tanstack/react-query";
import { fetchViewer } from "./api";

export const viewerQuery = () => queryOptions({ queryKey: ["viewer"], queryFn: () => fetchViewer() });
