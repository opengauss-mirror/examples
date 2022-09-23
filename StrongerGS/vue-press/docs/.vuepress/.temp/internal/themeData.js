export const themeData = JSON.parse("{\"colorMode\":\"dark\",\"colorModeSwitch\":true,\"sidebar\":false,\"navbar\":[{\"text\":\"首页\",\"link\":\"/\"},{\"text\":\"gstune\",\"link\":\"/gstune\"},{\"text\":\"gsnodejs\",\"link\":\"/gsnodejs\"},{\"text\":\"gsdiff\",\"link\":\"/gsdiff\"},{\"text\":\"gsbadger\",\"link\":\"/gsbadger\"},{\"text\":\"下载\",\"link\":\"/download\"}],\"locales\":{\"/\":{\"selectLanguageName\":\"English\"}},\"logo\":null,\"repo\":null,\"selectLanguageText\":\"Languages\",\"selectLanguageAriaLabel\":\"Select language\",\"sidebarDepth\":2,\"editLink\":true,\"editLinkText\":\"Edit this page\",\"lastUpdated\":true,\"lastUpdatedText\":\"Last Updated\",\"contributors\":true,\"contributorsText\":\"Contributors\",\"notFound\":[\"There's nothing here.\",\"How did we get here?\",\"That's a Four-Oh-Four.\",\"Looks like we've got some broken links.\"],\"backToHome\":\"Take me home\",\"openInNewWindow\":\"open in new window\",\"toggleColorMode\":\"toggle color mode\",\"toggleSidebar\":\"toggle sidebar\"}")

if (import.meta.webpackHot) {
  import.meta.webpackHot.accept()
  if (__VUE_HMR_RUNTIME__.updateThemeData) {
    __VUE_HMR_RUNTIME__.updateThemeData(themeData)
  }
}

if (import.meta.hot) {
  import.meta.hot.accept(({ themeData }) => {
    __VUE_HMR_RUNTIME__.updateThemeData(themeData)
  })
}
