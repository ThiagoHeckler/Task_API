import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import { TaskForm, type TaskFormValues } from './TaskForm'

const values: TaskFormValues = {
  title: 'Estudar Spring',
  description: '',
  priority: 'HIGH',
  dueDate: '2026-11-09T18:00',
  categoryId: '1',
  tagIds: '',
  parentTaskId: '',
}

function renderForm(mode: 'create' | 'edit') {
  render(
    <TaskForm
      mode={mode}
      suggested={values}
      current={mode === 'edit' ? values : undefined}
      categories={[{ id: 1, name: 'Estudos', color: '#4f46e5' }]}
      tags={[]}
      tasks={[]}
      errors={{ message: null, fields: {} }}
      saving={false}
      onSubmit={() => {}}
      onCancel={() => {}}
    />,
  )
}

// The API treats an empty field as "do not change", so editing cannot clear them.
test('editDoesNotOfferClearingCategory', () => {
  renderForm('edit')
  expect(screen.getByRole('option', { name: 'Sem categoria' })).toBeDisabled()
})

test('editRequiresDueDate', () => {
  renderForm('edit')
  expect(screen.getByLabelText('Prazo')).toBeRequired()
})

test('createKeepsCategoryAndDueDateOptional', () => {
  renderForm('create')
  expect(screen.getByRole('option', { name: 'Sem categoria' })).toBeEnabled()
  expect(screen.getByLabelText('Prazo')).not.toBeRequired()
})
