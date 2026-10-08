import { z } from 'zod'
import type { TaskInput } from '@/api/types'

/** Client-side mirror of the backend limits (title 200, description 5000, tag 30). No defaults on purpose. */
export const taskFormSchema = z.object({
  title: z.string().trim().min(1, 'Title is required.').max(200, 'Title must be at most 200 characters.'),
  description: z.string().max(5000, 'Description must be at most 5000 characters.'),
  priority: z.enum(['HIGH', 'MEDIUM', 'LOW']),
  dueDate: z.string().refine((value) => value === '' || /^\d{4}-\d{2}-\d{2}$/.test(value), 'Enter a valid date.'),
  assigneeId: z.string(),
  tagIds: z.array(z.string()),
  newTags: z.array(z.string().max(30, 'Tag names must be at most 30 characters.')),
})

export type TaskFormValues = z.infer<typeof taskFormSchema>

/** Sentinel for "nobody" in the assignee select (Radix selects cannot hold an empty value). */
export const UNASSIGNED = 'none'

export const TASK_FORM_FIELDS = ['title', 'description', 'priority', 'dueDate', 'assigneeId', 'tagIds', 'newTags']

export function toTaskInput(values: TaskFormValues): TaskInput {
  return {
    title: values.title.trim(),
    description: values.description.trim() || null,
    priority: values.priority,
    dueDate: values.dueDate || null,
    tagIds: values.tagIds,
    newTags: values.newTags,
  }
}
