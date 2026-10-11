import type { FormEvent, ReactNode } from 'react'

import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { PAGE_SIZE_OPTIONS, type FormField } from './formModel'
import { fieldChanged, formReset, submitSearch } from './searchSlice'

interface FieldProps {
  label: string
  name: FormField
  error?: string
  hint?: string
  children: ReactNode
}

function Field({ label, name, error, hint, children }: FieldProps) {
  return (
    <fieldset className="fieldset py-1">
      <label className="fieldset-legend pb-1 pt-0" htmlFor={name}>
        {label}
      </label>
      {children}
      {error ? (
        <p className="label text-error whitespace-normal" id={`${name}-error`} role="alert">
          {error}
        </p>
      ) : hint ? (
        <p className="label whitespace-normal">{hint}</p>
      ) : null}
    </fieldset>
  )
}

export function SearchForm() {
  const dispatch = useAppDispatch()
  const form = useAppSelector((s) => s.search.form)
  const errors = useAppSelector((s) => s.search.fieldErrors)
  const cities = useAppSelector((s) => s.search.cities)
  const loading = useAppSelector((s) => s.search.status === 'loading')

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    void dispatch(submitSearch())
  }

  const onReset = () => {
    dispatch(formReset())
    void dispatch(submitSearch())
  }

  const input = (name: FormField, props: { placeholder?: string; inputMode?: 'numeric' | 'text'; list?: string } = {}) => (
    <input
      id={name}
      name={name}
      type="text"
      className={`input input-sm w-full ${errors[name] ? 'input-error' : ''}`}
      value={form[name]}
      aria-invalid={Boolean(errors[name])}
      aria-describedby={errors[name] ? `${name}-error` : undefined}
      onChange={(e) => dispatch(fieldChanged({ field: name, value: e.target.value }))}
      {...props}
    />
  )

  return (
    <form onSubmit={onSubmit} noValidate aria-label="Search listings" className="flex flex-col gap-1">
      <Field label="Keyword" name="keyword" error={errors.keyword} hint="Matched against the description; all words must appear">
        {input('keyword', { placeholder: 'e.g. pool garage' })}
      </Field>

      <Field label="City" name="city" error={errors.city}>
        {input('city', { placeholder: 'Any city', list: 'city-options' })}
        <datalist id="city-options">
          {cities.map((c) => (
            <option key={c} value={c} />
          ))}
        </datalist>
      </Field>

      <div className="grid grid-cols-2 gap-3">
        <Field label="Min price" name="minPrice" error={errors.minPrice}>
          {input('minPrice', { placeholder: 'No min', inputMode: 'numeric' })}
        </Field>
        <Field label="Max price" name="maxPrice" error={errors.maxPrice}>
          {input('maxPrice', { placeholder: 'No max', inputMode: 'numeric' })}
        </Field>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <Field label="Min bedrooms" name="minBedrooms" error={errors.minBedrooms}>
          {input('minBedrooms', { placeholder: 'Any', inputMode: 'numeric' })}
        </Field>
        <Field label="Status" name="status" error={errors.status}>
          <select
            id="status"
            className="select select-sm w-full"
            value={form.status}
            onChange={(e) => dispatch(fieldChanged({ field: 'status', value: e.target.value }))}
          >
            <option value="">Any</option>
            <option value="active">Active</option>
            <option value="pending">Pending</option>
            <option value="sold">Sold</option>
          </select>
        </Field>
      </div>

      <div className="divider my-1 text-xs">Ranking</div>

      <Field
        label="Target budget"
        name="targetBudget"
        error={errors.targetBudget}
        hint="Ranks listings near this price higher (blended with recency)"
      >
        {input('targetBudget', { placeholder: 'e.g. 500000', inputMode: 'numeric' })}
      </Field>

      <Field label="Results per page" name="pageSize" error={errors.pageSize}>
        <select
          id="pageSize"
          className={`select select-sm w-full ${errors.pageSize ? 'select-error' : ''}`}
          value={form.pageSize}
          onChange={(e) => dispatch(fieldChanged({ field: 'pageSize', value: e.target.value }))}
        >
          {PAGE_SIZE_OPTIONS.map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
      </Field>

      <div className="mt-3 flex gap-2">
        <button type="submit" className="btn btn-primary btn-sm flex-1" disabled={loading}>
          {loading && <span className="loading loading-spinner loading-xs" />}
          Search
        </button>
        <button type="button" className="btn btn-ghost btn-sm" onClick={onReset} disabled={loading}>
          Reset
        </button>
      </div>
    </form>
  )
}
