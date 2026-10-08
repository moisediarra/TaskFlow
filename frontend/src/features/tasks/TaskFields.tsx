import { Controller, type Control, type FieldErrors, type UseFormRegister } from 'react-hook-form'
import type { ProjectMember, Tag } from '@/api/types'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { FormField } from '@/components/common/FormField'
import { PRIORITIES, PRIORITY_EMOJI, PRIORITY_LABEL } from '@/lib/labels'
import { TagPicker } from './TagPicker'
import { UNASSIGNED, type TaskFormValues } from './task-form'

interface TaskFieldsProps {
  idPrefix: string
  register: UseFormRegister<TaskFormValues>
  control: Control<TaskFormValues>
  errors: FieldErrors<TaskFormValues>
  tags: Tag[]
  canCreateTags: boolean
  /** Assignee picker is shown only to people allowed to assign (project owners). */
  members?: ProjectMember[]
}

/** Task fields of claude.md §13: title, description, priority, due date, tags and assignee. */
export function TaskFields({ idPrefix, register, control, errors, tags, canCreateTags, members }: TaskFieldsProps) {
  const id = (name: string) => `${idPrefix}-${name}`
  return (
    <div className="grid gap-4">
      <FormField label="Title" htmlFor={id('title')} error={errors.title?.message}>
        <Input id={id('title')} placeholder="Fix authentication API" aria-invalid={!!errors.title} {...register('title')} />
      </FormField>
      <FormField label="Description" htmlFor={id('description')} error={errors.description?.message}>
        <Textarea
          id={id('description')}
          rows={4}
          placeholder="Add details, acceptance criteria or links…"
          aria-invalid={!!errors.description}
          {...register('description')}
        />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Priority" htmlFor={id('priority')} error={errors.priority?.message}>
          <Controller
            control={control}
            name="priority"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger id={id('priority')} className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PRIORITIES.map((priority) => (
                    <SelectItem key={priority} value={priority}>
                      <span aria-hidden>{PRIORITY_EMOJI[priority]}</span> {PRIORITY_LABEL[priority]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </FormField>
        <FormField label="Due date" htmlFor={id('dueDate')} error={errors.dueDate?.message} hint="Optional">
          <Input id={id('dueDate')} type="date" aria-invalid={!!errors.dueDate} {...register('dueDate')} />
        </FormField>
      </div>
      {members ? (
        <FormField label="Assignee" htmlFor={id('assignee')} error={errors.assigneeId?.message}>
          <Controller
            control={control}
            name="assigneeId"
            render={({ field }) => (
              <Select value={field.value || UNASSIGNED} onValueChange={field.onChange}>
                <SelectTrigger id={id('assignee')} className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={UNASSIGNED}>Unassigned</SelectItem>
                  {members
                    .filter((member) => member.active)
                    .map((member) => (
                      <SelectItem key={member.userId} value={member.userId}>
                        {member.name}
                        {member.jobTitle ? <span className="text-muted-foreground"> · {member.jobTitle}</span> : null}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            )}
          />
        </FormField>
      ) : null}
      <FormField label="Tags" htmlFor={id('tags')} error={errors.tagIds?.message ?? errors.newTags?.message}>
        <Controller
          control={control}
          name="tagIds"
          render={({ field: tagField }) => (
            <Controller
              control={control}
              name="newTags"
              render={({ field: newTagField }) => (
                <TagPicker
                  id={id('tags')}
                  available={tags}
                  selectedIds={tagField.value}
                  newTags={newTagField.value}
                  canCreate={canCreateTags}
                  onChange={(ids, created) => {
                    tagField.onChange(ids)
                    newTagField.onChange(created)
                  }}
                />
              )}
            />
          )}
        />
      </FormField>
    </div>
  )
}
