'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { editedTagName, nextPreset, TAG_PRESETS } from '@/features/presets'
import { ErrorMessage, ItemActions, NO_ERRORS, SectionHeader, toFormErrors } from '@/features/system/ui'
import type { TagResponse } from '@/lib/types'
import { createTag, deleteTag, listTags, TAGS_KEY, updateTag, type TagInput } from './api'
import { TagForm } from './TagForm'

type FormState = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; tag: TagResponse }

type TagsPageProps = {
  onWaitingChange: (waiting: boolean) => void
}

export function TagsPage({ onWaitingChange }: TagsPageProps) {
  const queryClient = useQueryClient()
  const tags = useQuery({ queryKey: TAGS_KEY, queryFn: listTags })
  const [form, setForm] = useState<FormState>({ kind: 'closed' })
  // Bumped on every open so the form remounts and the typewriter starts over.
  const [formKey, setFormKey] = useState(0)

  // Every mutation is followed by a real GET of the list (visible in the timeline).
  const refreshList = () => queryClient.invalidateQueries({ queryKey: TAGS_KEY })

  const save = useMutation({
    mutationFn: (input: TagInput) =>
      form.kind === 'edit' ? updateTag(form.tag.id, input) : createTag(input),
    onSuccess: () => {
      setForm({ kind: 'closed' })
      return refreshList()
    },
  })

  const remove = useMutation({
    mutationFn: (id: number) => deleteTag(id),
    onSuccess: refreshList,
  })

  const waiting = form.kind !== 'closed' && !save.isPending
  useEffect(() => {
    onWaitingChange(waiting)
  }, [waiting, onWaitingChange])
  useEffect(() => () => onWaitingChange(false), [onWaitingChange])

  function open(next: FormState) {
    save.reset()
    setForm(next)
    setFormKey((key) => key + 1)
  }

  const list = tags.data ?? []

  return (
    <div className="flex flex-col gap-4">
      <SectionHeader
        title="Tags"
        actionLabel="Nova tag"
        onAction={() => open({ kind: 'create' })}
        actionDisabled={tags.isPending}
      />

      {form.kind !== 'closed' && (
        <TagForm
          key={formKey}
          suggested={
            form.kind === 'edit'
              ? { name: editedTagName(form.tag.name) }
              : nextPreset(
                  TAG_PRESETS,
                  list.map((tag) => tag.name),
                )
          }
          current={form.kind === 'edit' ? { name: form.tag.name } : undefined}
          errors={save.isError ? toFormErrors(save.error) : NO_ERRORS}
          saving={save.isPending}
          onSubmit={(input) => save.mutate(input)}
          onCancel={() => setForm({ kind: 'closed' })}
        />
      )}

      {tags.isPending && <p className="text-slate-500">Carregando…</p>}
      {tags.isError && <ErrorMessage message={toFormErrors(tags.error).message} />}
      {remove.isError && <ErrorMessage message={toFormErrors(remove.error).message} />}
      {tags.isSuccess && list.length === 0 && <p className="text-slate-500">Nenhuma tag ainda</p>}

      <ul className="flex flex-col divide-y divide-slate-200">
        {list.map((tag) => (
          <li key={tag.id} className="flex items-center justify-between gap-3 py-2">
            <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-sm text-slate-800">
              {tag.name}
            </span>
            <ItemActions
              label={tag.name}
              onEdit={() => open({ kind: 'edit', tag })}
              onDelete={() => remove.mutate(tag.id)}
              deleting={remove.isPending}
            />
          </li>
        ))}
      </ul>
    </div>
  )
}
