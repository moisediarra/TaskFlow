import { expect, type Browser, type Page } from '@playwright/test'

export const PASSWORD = 'E2e-pass-1234'
export const RUN = Date.now().toString(36)

export function emailFor(name: string) {
  return `${name.toLowerCase().replace(/\s+/g, '.')}.${RUN}@e2e.test`
}

export async function newPage(browser: Browser) {
  const context = await browser.newContext()
  return context.newPage()
}

export async function register(page: Page, name: string, email: string) {
  await page.goto('/register')
  await page.getByLabel('Full name').fill(name)
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD)
  await page.getByLabel('Confirm password').fill(PASSWORD)
  await page.getByRole('button', { name: 'Create account' }).click()
  await expect(page).toHaveURL(/\/login/)
}

export async function login(page: Page, email: string, password = PASSWORD) {
  if (!page.url().includes('/login')) await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page).not.toHaveURL(/\/login/)
}

/** Creates a project from the Projects page and returns its id. */
export async function createProject(page: Page, name: string) {
  await page.goto('/projects')
  await page.getByRole('button', { name: /New project|Create Project/ }).first().click()
  await page.getByLabel('Name').fill(name)
  await page.getByRole('button', { name: 'Create project' }).click()
  await expect(page).toHaveURL(/\/projects\/[0-9a-f-]{36}$/)
  return page.url().split('/').pop()!
}

export async function addMember(page: Page, projectId: string, email: string, name: string) {
  await page.goto(`/projects/${projectId}/settings`)
  await page.getByLabel('Email').fill(email)
  await page.getByRole('button', { name: 'Add member' }).click()
  await expect(page.getByRole('button', { name: `Remove ${name}` })).toBeVisible()
}

export async function createTaskIn(page: Page, projectId: string, column: string, title: string) {
  await page.goto(`/projects/${projectId}`)
  await page.getByRole('button', { name: `Add a task to ${column}` }).click()
  await page.getByLabel('Title').fill(title)
  await page.getByRole('button', { name: 'Create task' }).click()
  await expect(column_(page, column).getByText(title)).toBeVisible()
}

export function column_(page: Page, column: string) {
  return page.getByRole('region', { name: `${column} column` })
}

export function card(page: Page, title: string) {
  return page.getByRole('button', { name: new RegExp(`^${title.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\.`) })
}

/** Picks an option of a labelled Radix select. */
export async function choose(page: Page, label: string, option: string) {
  await page.getByRole('combobox', { name: label }).click()
  await page.getByRole('option', { name: option, exact: true }).click()
}
