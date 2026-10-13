import { expect, test, type Browser } from '@playwright/test'
import { RUN, cspViolations, emailFor, login, newPage } from './helpers'

const IT_EMAIL = process.env.E2E_IT_EMAIL
const IT_PASSWORD = process.env.E2E_IT_PASSWORD
const STARTER = 'Starter-pass-1'
const RESET = 'Reset-pass-2'

/** claude.md §31: the IT Manager manages accounts; every change asks for their password. */
test('IT Manager adds, edits, resets the password of and deletes an account', async ({ browser }) => {
  test.skip(!IT_EMAIL || !IT_PASSWORD, 'Set E2E_IT_EMAIL and E2E_IT_PASSWORD to an IT Manager account')
  const name = `Nina Newhire ${RUN}`
  const email = emailFor('nina')

  const it = await newPage(browser)
  await login(it, IT_EMAIL!, IT_PASSWORD!)

  // Add the account; the IT Manager lands on its page.
  await it.goto('/management/users')
  await it.getByRole('button', { name: 'Add user' }).click()
  const add = it.getByRole('dialog')
  await add.getByLabel('Full name').fill(name)
  await add.getByLabel('Email').fill(email)
  await add.getByLabel('Job title').fill('Developer')
  await add.getByLabel('Starting password').fill(STARTER)
  await add.getByLabel('Confirm password').fill(STARTER)
  await add.getByLabel('Your password').fill(IT_PASSWORD!)
  await add.getByRole('button', { name: 'Create account' }).click()
  await expect(it).toHaveURL(/\/management\/users\/[0-9a-f-]{36}$/)
  await expect(it.getByRole('heading', { level: 1, name })).toBeVisible()

  // The new person signs in with the starting password.
  const nina = await newPage(browser)
  await login(nina, email, STARTER)
  await expect(nina).toHaveURL(/\/dashboard/)

  // Correct the details.
  await it.getByRole('button', { name: 'Edit details' }).click()
  const edit = it.getByRole('dialog')
  await edit.getByLabel('Job title').fill('Senior Developer')
  await edit.getByLabel('Your password').fill(IT_PASSWORD!)
  await edit.getByRole('button', { name: 'Save changes' }).click()
  await expect(it.getByText(`${email} · Senior Developer`)).toBeVisible()

  // Reset the password: the starting one stops working, the new one works.
  await it.getByRole('button', { name: 'Reset password' }).click()
  const reset = it.getByRole('dialog')
  await reset.getByLabel('New password', { exact: true }).fill(RESET)
  await reset.getByLabel('Confirm new password').fill(RESET)
  await reset.getByLabel('Your password').fill(IT_PASSWORD!)
  await reset.getByRole('button', { name: 'Reset password' }).click()
  await expect(it.getByText(`${name}'s password was reset.`)).toBeVisible()
  await expectSignInRefused(browser, email, STARTER)
  await login(await newPage(browser), email, RESET)

  // Delete the account: it disappears and can no longer sign in.
  await it.getByRole('button', { name: 'Delete account' }).click()
  const remove = it.getByRole('dialog')
  await remove.getByLabel('Your password').fill(IT_PASSWORD!)
  await remove.getByRole('button', { name: 'Delete account' }).click()
  await expect(it).toHaveURL(/\/management\/users$/)
  await expect(it.getByText(`${name}'s account was deleted.`)).toBeVisible()
  await expectSignInRefused(browser, email, RESET)

  // Each change is in the activity log.
  await it.goto('/management/activity-logs')
  for (const change of [`created an account for ${name} (Member).`, `updated ${name}'s account (job title).`, `reset ${name}'s password.`, `deleted ${name}'s account.`]) {
    await expect(it.getByText(change, { exact: false })).toBeVisible()
  }
  expect(cspViolations).toEqual([])
})

async function expectSignInRefused(browser: Browser, email: string, password: string) {
  const page = await newPage(browser)
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByText('Invalid email or password.')).toBeVisible()
  await page.context().close()
}
