import { expect, test } from '@playwright/test'
import { RUN, cspViolations, addMember, card, choose, createProject, createTaskIn, emailFor, login, newPage, register, setStatus } from './helpers'

const IT_EMAIL = process.env.E2E_IT_EMAIL
const IT_PASSWORD = process.env.E2E_IT_PASSWORD

/** claude.md §45: the workflow that defines "done", with three people in three browser sessions. */
test('definition of done: register → project → task → assignment → progress → completion', async ({ browser }) => {
  test.skip(!IT_EMAIL || !IT_PASSWORD, 'Set E2E_IT_EMAIL and E2E_IT_PASSWORD to an IT Manager account')

  const ownerName = `Olivia Owner ${RUN}`
  const memberName = `Ian Intervenant ${RUN}`
  const ownerEmail = emailFor('olivia')
  const memberEmail = emailFor('ian')
  const taskTitle = `Implement Login UI ${RUN}`

  // User registers and logs in (and so does the intervenant).
  const owner = await newPage(browser)
  await register(owner, ownerName, ownerEmail)
  await login(owner, ownerEmail)
  await expect(owner).toHaveURL(/\/dashboard/)
  await expect(owner.getByRole('heading', { name: /Olivia/ })).toBeVisible()

  const member = await newPage(browser)
  await register(member, memberName, memberEmail)
  await login(member, memberEmail)

  // User creates a project and adds the intervenant.
  const projectId = await createProject(owner, `Mobile Banking App ${RUN}`)
  await addMember(owner, projectId, memberEmail, memberName)

  // User creates a task: it is placed in Backlog.
  await createTaskIn(owner, projectId, 'Backlog', taskTitle)

  // Task is assigned to the intervenant.
  await card(owner, taskTitle).click()
  await choose(owner, 'Assignee', memberName)
  await expect(owner.getByText(`Assigned to ${memberName}.`)).toBeVisible()
  await owner.keyboard.press('Escape')

  // Intervenant receives a notification in real time, without reloading.
  await expect(member.getByText(/assigned you a task/).first()).toBeVisible({ timeout: 15_000 })
  await expect(member.getByRole('button', { name: 'Notifications, 1 unread' })).toBeVisible()

  // Task moves to To Do, then to In Progress (by the intervenant).
  await member.goto(`/projects/${projectId}`)
  await setStatus(member, taskTitle, 'To Do')
  await setStatus(member, taskTitle, 'In Progress')

  // The IT Manager sees who is working on it, in Team Activity.
  const it = await newPage(browser)
  await login(it, IT_EMAIL!, IT_PASSWORD!)
  await expect(it).toHaveURL(/\/management/)
  await it.goto('/management/team-activity')
  const row = it.getByRole('row').filter({ hasText: taskTitle })
  await expect(row).toContainText(memberName)
  await expect(row).toContainText('In Progress')

  // Task is completed: it moves to Done.
  await setStatus(member, taskTitle, 'Done')

  // Activity is recorded.
  await it.goto('/management/activity-logs')
  await expect(it.getByText(`${memberName} completed "${taskTitle}".`)).toBeVisible()

  // Relevant users receive notifications: the owner was told about each status change.
  await owner.reload()
  await owner.getByRole('button', { name: /Notifications, \d+ unread/ }).click()
  await expect(owner.getByText(`${memberName} completed a task`)).toBeVisible()
  expect(cspViolations).toEqual([])
})
