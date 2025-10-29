import { route } from 'quasar/wrappers'
import { createRouter, createMemoryHistory, createWebHistory, createWebHashHistory } from 'vue-router'
import routes from './routes'

/*
 * If not building with SSR mode, you can
 * directly export the Router instantiation;
 *
 * The function below can be async too; either use
 * async/await or return a Promise which resolves
 * with the Router instance.
 */

// routes/index.js
export default route(function () {
  const Router = createRouter({
    scrollBehavior: () => ({ top: 0 }),
    routes,
    history: createWebHistory('/static-plugin/er-model/')
  })
  return Router
})

