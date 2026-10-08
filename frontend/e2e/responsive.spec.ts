import { expect, test, type Page } from '@playwright/test'
import { RUN, createProject, createTaskIn, emailFor, login, newPage, register } from './helpers'

const IT_EMAIL = process.env.E2E_IT_EMAIL
const IT_PASSWORD = process.env.E2E_IT_PASSWORD
const WIDTHS = [375, 768, 1280]

/** claude.md §34: every page fits the screen at phone, tablet and desktop widths (the board scrolls inside itself). */
test.describe.serial('responsive layout', () => {
  const email = emailFor('responsive')
  let projectId = ''

  test.beforeAll(async ({ browser }) => {
    const page = await newPage(browser)
    await register(page, `Rita Responsive ${RUN}`, email)
    await login(page, email)
    projectId = await createProject(page, `Responsive check ${RUN}`)
    await createTaskIn(page, projectId, 'To Do', `Check the layout on small screens ${RUN}`)
    await page.context().close()
  })

  for (const width of WIDTHS) {
    test(`pages fit at ${width}px`, async ({ browser }, testInfo) => {
      const context = await browser.newContext({ viewport: { width, height: width < 800 ? 812 : 900 } })
      const page = await context.newPage()

      await page.goto('/login')
      await expectFits(page, width, '/login', testInfo.outputPath(`${width}-login.png`))

      await login(page, email)
      const pages = ['/dashboard', '/projects', `/projects/${projectId}`, `/projects/${projectId}/overview`,
        `/projects/${projectId}/settings`, '/notifications', '/profile']
      for (const path of pages) await visit(page, width, path, testInfo.outputPath(`${width}-${slug(path, projectId)}.png`))

      await page.goto(`/projects/${projectId}`)
      await page.getByRole('button', { name: /^Check the layout on small screens/ }).click()
      await expect(page.getByRole('dialog')).toBeVisible()
      await expectFits(page, width, 'task sheet', testInfo.outputPath(`${width}-task-sheet.png`))
      await context.close()

      if (IT_EMAIL && IT_PASSWORD) {
        const itContext = await browser.newContext({ viewport: { width, height: width < 800 ? 812 : 900 } })
        const it = await itContext.newPage()
        await login(it, IT_EMAIL, IT_PASSWORD)
        const management = ['/management', '/management/team-activity', '/management/workload',
          '/management/activity-logs', '/management/users', '/management/projects']
        for (const path of management) await visit(it, width, path, testInfo.outputPath(`${width}-${slug(path, projectId)}.png`))
        await itContext.close()
      }
    })
  }
})

async function visit(page: Page, width: number, path: string, screenshot: string) {
  await page.goto(path)
  await page.waitForLoadState('networkidle')
  await expectFits(page, width, path, screenshot)
}

async function expectFits(page: Page, width: number, label: string, screenshot: string) {
  await page.screenshot({ path: screenshot, fullPage: true })
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(overflow, `${label} scrolls sideways at ${width}px`).toBeLessThanOrEqual(0)
}

function slug(path: string, projectId: string) {
  return path.replace(projectId, 'project').replace(/^\//, '').replace(/\//g, '_')
}
