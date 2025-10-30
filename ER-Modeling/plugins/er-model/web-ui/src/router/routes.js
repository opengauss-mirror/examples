const routes = [
  {
    path: "/",
    component: () => import("layouts/MainLayout.vue"),
    redirect: '/editor',
    children: [
      {
        path: "",
        redirect: "/editor"
      },
      {
        path: "editor",
        components: {
          default: () => import("pages/Editor/Index.vue"),
          toolbar: () => import("pages/Editor/Toolbar.vue")
        }
      },
      {
        path: 'show-diagram', // This creates the full URL
        component: () => import('pages/ShowDiagramPage.vue') // Point to the new page component
      }
    ]
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: "/:catchAll(.*)*",
    component: () => import("pages/Error404.vue")
  }
];

export default routes;
