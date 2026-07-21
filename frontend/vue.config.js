const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    host: '0.0.0.0',          // 允许局域网访问
    port: 8081,
    allowedHosts: 'all',      // 允许任何 Host 头
    client: {
      webSocketURL: 'auto://0.0.0.0:0/ws'  // 自动匹配 ws / wss
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
