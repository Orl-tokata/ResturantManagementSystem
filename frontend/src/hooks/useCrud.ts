"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
} from "@tanstack/react-query";
import { del, get, post, put, type PageResponse } from "@/lib/api";

/**
 * The five list screens share one shape: paged GET with filters, POST, PUT,
 * DELETE. These hooks capture that once so each page only describes its
 * columns and its form.
 */

export interface ListParams {
  search?: string;
  page?: number;
  size?: number;
  [key: string]: string | number | undefined;
}

/** Drops undefined and empty values so they never reach the query string. */
function clean(params: ListParams): Record<string, string | number> {
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== ""),
  ) as Record<string, string | number>;
}

export function useList<T>(resource: string, params: ListParams = {}) {
  const query = clean(params);
  return useQuery({
    queryKey: [resource, query],
    queryFn: () => get<PageResponse<T>>(`/${resource}`, query),
    // Keeps the previous page visible while the next one loads, instead of
    // collapsing the table to a spinner on every keystroke or page change.
    placeholderData: (prev) => prev,
  });
}

export function useAll<T>(resource: string, path = "active") {
  return useQuery({
    queryKey: [resource, path],
    queryFn: () => get<T[]>(`/${resource}/${path}`),
    staleTime: 5 * 60_000,
  });
}

/**
 * Create when `id` is null, update otherwise — the same modal handles both,
 * so the caller does not choose between two mutations.
 */
export function useSave<TResponse, TRequest>(
  resource: string,
): UseMutationResult<TResponse, unknown, { id: number | null; body: TRequest }> {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }) =>
      id === null
        ? post<TResponse>(`/${resource}`, body)
        : put<TResponse>(`/${resource}/${id}`, body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: [resource] });
    },
  });
}

export function useRemove(resource: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => del<void>(`/${resource}/${id}`),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: [resource] });
    },
  });
}
