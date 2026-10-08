# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: definition-of-done.spec.ts >> definition of done: register → project → task → assignment → progress → completion
- Location: e2e\definition-of-done.spec.ts:8:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('region', { name: 'To Do column' }).getByText('Implement Login UI muzfsug6')
Expected: visible
Timeout: 10000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" getByRole('region', { name: 'To Do column' }).getByText('Implement Login UI muzfsug6') with timeout 10000ms
  - waiting for getByRole('region', { name: 'To Do column' }).getByText('Implement Login UI muzfsug6')

```

```yaml
- main:
  - paragraph
  - status
- region "Notifications alt+T"
- dialog "Implement Login UI muzfsug6":
  - text: To Do Medium
  - heading "Implement Login UI muzfsug6" [level=2]
  - paragraph: in Mobile Banking App muzfsug6
  - heading "Description" [level=3]
  - paragraph: No description.
  - term: Status
  - definition:
    - combobox "Status": To Do
  - term: Priority
  - definition: Medium
  - term: Due date
  - definition: No due date
  - term: Assignee
  - definition: IM Ian Intervenant muzfsug6
  - term: Tags
  - definition: No tags
  - term: Created
  - definition: October 8
  - term: Last updated
  - definition: October 8
  - button "Edit task"
  - button "Close"
```

# Test source

```ts
  1  | import { expect, test } from '@playwright/test'
  2  | import { RUN, addMember, card, choose, column_, createProject, createTaskIn, emailFor, login, newPage, register } from './helpers'
  3  | 
  4  | const IT_EMAIL = process.env.E2E_IT_EMAIL
  5  | const IT_PASSWORD = process.env.E2E_IT_PASSWORD
  6  | 
  7  | /** claude.md §45: the workflow that defines "done", with three people in three browser sessions. */
  8  | test('definition of done: register → project → task → assignment → progress → completion', async ({ browser }) => {
  9  |   test.skip(!IT_EMAIL || !IT_PASSWORD, 'Set E2E_IT_EMAIL and E2E_IT_PASSWORD to an IT Manager account')
  10 | 
  11 |   const ownerName = `Olivia Owner ${RUN}`
  12 |   const memberName = `Ian Intervenant ${RUN}`
  13 |   const ownerEmail = emailFor('olivia')
  14 |   const memberEmail = emailFor('ian')
  15 |   const taskTitle = `Implement Login UI ${RUN}`
  16 | 
  17 |   // User registers and logs in (and so does the intervenant).
  18 |   const owner = await newPage(browser)
  19 |   await register(owner, ownerName, ownerEmail)
  20 |   await login(owner, ownerEmail)
  21 |   await expect(owner).toHaveURL(/\/dashboard/)
  22 |   await expect(owner.getByRole('heading', { name: /Olivia/ })).toBeVisible()
  23 | 
  24 |   const member = await newPage(browser)
  25 |   await register(member, memberName, memberEmail)
  26 |   await login(member, memberEmail)
  27 | 
  28 |   // User creates a project and adds the intervenant.
  29 |   const projectId = await createProject(owner, `Mobile Banking App ${RUN}`)
  30 |   await addMember(owner, projectId, memberEmail, memberName)
  31 | 
  32 |   // User creates a task: it is placed in Backlog.
  33 |   await createTaskIn(owner, projectId, 'Backlog', taskTitle)
  34 | 
  35 |   // Task is assigned to the intervenant.
  36 |   await card(owner, taskTitle).click()
  37 |   await choose(owner, 'Assignee', memberName)
  38 |   await expect(owner.getByText(`Assigned to ${memberName}.`)).toBeVisible()
  39 |   await owner.keyboard.press('Escape')
  40 | 
  41 |   // Intervenant receives a notification in real time, without reloading.
  42 |   await expect(member.getByText(/assigned you a task/).first()).toBeVisible({ timeout: 15_000 })
  43 |   await expect(member.getByRole('button', { name: 'Notifications, 1 unread' })).toBeVisible()
  44 | 
  45 |   // Task moves to To Do, then to In Progress (by the intervenant).
  46 |   await member.goto(`/projects/${projectId}`)
  47 |   await card(member, taskTitle).click()
  48 |   await choose(member, 'Status', 'To Do')
> 49 |   await expect(column_(member, 'To Do').getByText(taskTitle)).toBeVisible()
     |                                                               ^ Error: expect(locator).toBeVisible() failed
  50 |   await choose(member, 'Status', 'In Progress')
  51 |   await expect(column_(member, 'In Progress').getByText(taskTitle)).toBeVisible()
  52 |   await member.keyboard.press('Escape')
  53 | 
  54 |   // The IT Manager sees who is working on it, in Team Activity.
  55 |   const it = await newPage(browser)
  56 |   await login(it, IT_EMAIL!, IT_PASSWORD!)
  57 |   await expect(it).toHaveURL(/\/management/)
  58 |   await it.goto('/management/team-activity')
  59 |   const row = it.getByRole('row').filter({ hasText: taskTitle })
  60 |   await expect(row).toContainText(memberName)
  61 |   await expect(row).toContainText('In Progress')
  62 | 
  63 |   // Task is completed: it moves to Done.
  64 |   await card(member, taskTitle).click()
  65 |   await choose(member, 'Status', 'Done')
  66 |   await expect(column_(member, 'Done').getByText(taskTitle)).toBeVisible()
  67 | 
  68 |   // Activity is recorded.
  69 |   await it.goto('/management/activity-logs')
  70 |   await expect(it.getByText(`${memberName} completed "${taskTitle}".`)).toBeVisible()
  71 | 
  72 |   // Relevant users receive notifications: the owner was told about each status change.
  73 |   await owner.reload()
  74 |   await owner.getByRole('button', { name: /Notifications, \d+ unread/ }).click()
  75 |   await expect(owner.getByText(`${memberName} completed a task`)).toBeVisible()
  76 | })
  77 | 
```