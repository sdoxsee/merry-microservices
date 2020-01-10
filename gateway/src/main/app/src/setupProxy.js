// https://create-react-app.dev/docs/proxying-api-requests-in-development#configuring-the-proxy-manually
// https://github.com/chimurai/http-proxy-middleware
const proxy = require('http-proxy-middleware');
module.exports = function(app) {
  app.use(
    [
        '/api',
        '/logout',
        '/private',
        '/oauth2/authorization/login-client',
        '/login/oauth2/code/login-client'
    ],
    proxy({
      target: 'http://localhost:8082',
      changeOrigin: true,
      xfwd: true,
    })
  );
};