import { expect, test } from 'vitest'
import { CATEGORY_PRESETS, nextPreset } from './presets'

test('picksFirstUnusedPreset', () => {
  expect(nextPreset(CATEGORY_PRESETS, ['Estudos']).name).toBe('Trabalho')
})

test('addsNumericSuffixWhenPresetsRunOut', () => {
  const used = ['Estudos', 'Trabalho', 'Pessoal', 'Estudos 2']

  expect(nextPreset(CATEGORY_PRESETS, used)).toEqual({ name: 'Trabalho 2', color: '#0ea5e9' })
})
