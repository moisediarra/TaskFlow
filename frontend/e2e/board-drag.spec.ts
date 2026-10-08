import { expect, test } from '@playwright/test'
import { RUN, card, column_, createProject, createTaskIn, emailFor, login, newPage, register } from './helpers'

/** claude.md §11: dragging a card persists its new column (it survives a reload). */
test('drag and drop moves a card to another column and persists it', async ({ browser }) => {
  const page = await newPage(browser)
  const email = emailFor('dragger')
  await register(page, `Drag Owner ${RUN}`, email)
  await login(page, email)
  const projectId = await createProject(page, `Drag board ${RUN}`)
  const title = `Draggable task ${RUN}`
  await createTaskIn(page, projectId, 'To Do', title)

  const source = await card(page, title).boundingBox()
  const target = await column_(page, 'In Progress').boundingBox()
  if (!source || !target) throw new Error('Board is not laid out')

  await page.mouse.move(source.x + source.width / 2, source.y + source.height / 2)
  await page.mouse.down()
  await page.mouse.move(source.x + source.width / 2 + 12, source.y + source.height / 2 + 12, { steps: 4 })
  await page.mouse.move(target.x + target.width / 2, target.y + 90, { steps: 20 })
  await page.mouse.up()

  await expect(column_(page, 'In Progress').getByText(title)).toBeVisible()
  await page.reload()
  await expect(column_(page, 'In Progress').getByText(title)).toBeVisible()
  await expect(column_(page, 'To Do').getByText(title)).toHaveCount(0)
})
