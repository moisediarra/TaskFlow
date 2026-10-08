import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/api/client'
import { Toaster } from '@/components/ui/sonner'
import { TooltipProvider } from '@/components/ui/tooltip'
import { AuthProvider } from '@/features/auth/AuthProvider'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: true,
      // Retrying cannot fix authorization or missing data; it only delays the error message.
      retry: (failureCount, error) =>
        !(error instanceof ApiError && [400, 401, 403, 404].includes(error.status)) && failureCount < 2,
    },
    mutations: { retry: false },
  },
})

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider delayDuration={200}>
        <AuthProvider>{children}</AuthProvider>
        <Toaster position="top-right" closeButton />
      </TooltipProvider>
    </QueryClientProvider>
  )
}
