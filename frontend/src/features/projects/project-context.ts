import { useOutletContext } from 'react-router'
import type { ProjectDetail } from '@/api/types'

export interface ProjectOutletContext {
  project: ProjectDetail
}

/** The project loaded by ProjectLayout, for its Board, Overview and Settings tabs. */
export function useProject() {
  return useOutletContext<ProjectOutletContext>().project
}
