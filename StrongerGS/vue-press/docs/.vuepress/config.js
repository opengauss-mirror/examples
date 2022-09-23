const {defaultTheme} = require('vuepress')


module.exports = {
    lang: 'zh-CN',
    title: 'StrongerGS工具箱！',
    description: 'StrongerGS工具箱:一个面向openGauss数据库的工具箱',
    theme: defaultTheme({
        // 默认主题配置
        colorMode: "dark",
        colorModeSwitch: true,
        sidebar: false,
        navbar: [
            {
                text: '首页',
                link: '/',
            }, {
                text: 'gstune',
                link: '/gstune',
            }, {
                text: 'gsnodejs',
                link: '/gsnodejs',
            }, {
                text: 'gsdiff',
                link: '/gsdiff',
            }, {
                text: 'gsbadger',
                link: '/gsbadger',
            }, {
                text: '下载',
                link: '/download',
            },
        ],
    }),
}
