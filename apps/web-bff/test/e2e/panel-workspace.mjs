import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';

const { chromium } = createRequire(process.env.WH_PLAYWRIGHT_MODULE)('playwright');
const app = new URL('../../public/', import.meta.url);
const pageUrl = 'https://whitehat.test';
const titles = { first: 'First workspace decision', second: 'Second workspace decision' };
const ids = { first: 'decision-1', second: 'decision-2' };
const decision = workspace => ({
  id: ids[workspace], title: titles[workspace], decision: titles[workspace], mode: 'challenge', revision: 1,
});

const browser = await chromium.launch({
  executablePath: process.env.WH_BROWSER_BIN,
  args: ['--no-sandbox', '--single-process', '--no-zygote'],
});
try {
  const page = await browser.newPage();
  page.setDefaultTimeout(5000);
  let heldRequest;
  let markPending;
  let releaseResponse;
  const pending = new Promise(resolve => { markPending = resolve; });
  const response = new Promise(resolve => { releaseResponse = resolve; });

  await page.route(`${pageUrl}/**`, async route => {
    const path = new URL(route.request().url()).pathname;
    const files = { '/': ['index.html', 'text/html'], '/panel.js': ['panel.js', 'text/javascript'], '/panel.css': ['panel.css', 'text/css'] };
    if (files[path]) {
      const [file, contentType] = files[path];
      return route.fulfill({ contentType, body: await readFile(new URL(file, app)) });
    }
    if (path === '/bff/session') return route.fulfill({ json: { csrf: 'fixture' } });
    if (path === '/bff/api/v1/me') {
      const workspaces = Object.keys(titles).map(id => ({ id, name: id, role: 'OWNER' }));
      return route.fulfill({ json: { subject: 'fixture', workspaces } });
    }
    if (path === '/bff/api/v1/modes') return route.fulfill({ json: { items: [{ id: 'challenge' }] } });
    const match = path.match(/^\/bff\/api\/v1\/workspaces\/(first|second)\/decisions(?:\/([^/]+))?$/);
    if (!match) return route.fulfill({ status: 404, json: { error: 'NOT_FOUND' } });
    const [, workspace, id] = match;
    if (!id) return route.fulfill({ json: { items: [decision(workspace)] } });
    if (workspace === 'first') {
      heldRequest = route.request();
      markPending();
      await response;
    }
    return route.fulfill({ json: decision(workspace) });
  });

  await page.goto(pageUrl);
  const history = page.locator('nav [data-screen="history"]');
  await history.click();
  await page.locator(`[data-decision="${ids.first}"]`).click();
  await pending;
  await page.locator('#workspaces').selectOption('second');
  await history.click();
  await page.locator(`[data-decision="${ids.second}"]`).waitFor();
  const finished = page.waitForEvent('requestfinished', { predicate: request => request === heldRequest });
  releaseResponse();
  await finished;
  // A same-page fetch barrier lets the earlier response and its UI continuation settle.
  await page.evaluate(() => fetch('/bff/session').then(response => response.json()));
  assert.equal(await page.locator('#workspaces').inputValue(), 'second');
  assert.equal(await page.locator('main h1').textContent(), 'آرشیو تصمیم‌ها');
  assert.equal(await page.getByText(titles.first, { exact: true }).count(), 0);
  await page.locator(`[data-decision="${ids.second}"]`).click();
  await page.getByRole('heading', { name: new RegExp(titles.second) }).waitFor();
  console.log(JSON.stringify({ check: 'browser_workspace_ignores_stale_decision', pass: true }));
} finally {
  await browser.close();
}
