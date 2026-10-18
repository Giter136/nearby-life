/*
 * 近邻生活 · API 访问层
 * ---------------------------------------------------------------
 * 本文件是前端访问后端的唯一入口，所有请求路径、请求方式、
 * 查询参数均与后端既有控制器一一对应，不做合并、改名或新增。
 *
 * 响应约定：
 *   { success: Boolean, errorMsg: String, data: Any, total: Long }
 *   - 成功时 resolve 后端 data 字段
 *   - 失败时 reject Error（message 为后端 errorMsg）
 *   - HTTP 401 时 reject Error 并标记 err.auth = true
 *
 * 注意：拦截器不再“全局 401 就跳登录页”。
 * 后端只放行 /user/code、/user/login、/blog/hot、/shop/**、
 * /voucher/list/**；其余接口对游客返回 401。哪些页面需要登录
 * 应由页面按业务决定，而不是让一次辅助请求把整页踢走。
 */
(function (global) {
  'use strict';

  var BASE_URL = '/api';
  var TOKEN_KEY = 'token';

  function makeError(message, auth, status) {
    var err = new Error(message || '请求失败');
    err.auth = !!auth;
    err.status = status || 0;
    return err;
  }

  var http = axios.create({
    baseURL: BASE_URL,
    timeout: 12000,
    // 与旧版 common.js 一致：过滤空值，且不对参数做 URL 编码，
    // 以保证 /upload/blog/delete?name=/imgs/... 这类路径原样传递。
    paramsSerializer: function (params) {
      var pairs = [];
      Object.keys(params || {}).forEach(function (key) {
        var value = params[key];
        if (value !== undefined && value !== null && value !== '') {
          pairs.push(key + '=' + value);
        }
      });
      return pairs.join('&');
    }
  });

  http.interceptors.request.use(function (config) {
    var token = sessionStorage.getItem(TOKEN_KEY);
    if (token) config.headers['authorization'] = token;
    return config;
  });

  http.interceptors.response.use(
    function (response) {
      var body = response.data;
      if (!body || body.success !== true) {
        return Promise.reject(makeError((body && body.errorMsg) || '请求失败', false, response.status));
      }
      return body;
    },
    function (error) {
      var status = error && error.response && error.response.status;
      if (status === 401) {
        return Promise.reject(makeError('请先登录', true, 401));
      }
      if (status === 404) {
        return Promise.reject(makeError('接口不存在', false, 404));
      }
      return Promise.reject(makeError('服务器异常', false, status || 0));
    }
  );

  function data(promise) {
    return promise.then(function (res) {
      return res.data;
    });
  }

  var api = {
    isAuthError: function (err) {
      return !!(err && err.auth);
    },
    isNotFound: function (err) {
      return !!(err && err.status === 404);
    },
    /* ---------------- 商户类型 ---------------- */
    // GET /shop-type/list
    shopTypes: function () {
      return data(http.get('/shop-type/list'));
    },

    /* ---------------- 笔记 ---------------- */
    // GET /blog/hot?current=
    hotBlogs: function (current) {
      return data(http.get('/blog/hot', { params: { current: current } }));
    },
    // GET /blog/{id}
    blog: function (id) {
      return data(http.get('/blog/' + id));
    },
    // PUT /blog/like/{id}
    likeBlog: function (id) {
      return data(http.put('/blog/like/' + id));
    },
    // GET /blog/likes/{id}
    blogLikes: function (id) {
      return data(http.get('/blog/likes/' + id));
    },
    // GET /blog/of/me
    myBlogs: function () {
      return data(http.get('/blog/of/me'));
    },
    // GET /blog/of/user?id=&current=
    userBlogs: function (id, current) {
      return data(http.get('/blog/of/user', { params: { id: id, current: current } }));
    },
    // GET /blog/of/follow?offset=&lastId=
    followFeed: function (offset, lastId) {
      return data(http.get('/blog/of/follow', { params: { offset: offset, lastId: lastId } }));
    },
    // POST /blog
    publishBlog: function (payload) {
      return data(http.post('/blog', payload));
    },

    /* ---------------- 商户 ---------------- */
    // GET /shop/{id}
    shop: function (id) {
      return data(http.get('/shop/' + id));
    },
    // GET /shop/of/type?typeId=&current=&sortBy=&x=&y=
    shopsByType: function (params) {
      return data(http.get('/shop/of/type', { params: params }));
    },
    // GET /shop/of/name?name=
    shopsByName: function (name) {
      return data(http.get('/shop/of/name', { params: { name: name } }));
    },

    /* ---------------- 图片上传 ---------------- */
    // POST /upload/blog
    uploadBlogImage: function (formData) {
      // 交给浏览器为 FormData 生成 multipart boundary
      return data(http.post('/upload/blog', formData, { timeout: 30000 }));
    },
    // GET /upload/blog/delete?name=
    deleteBlogImage: function (name) {
      return data(http.get('/upload/blog/delete', { params: { name: name } }));
    },

    /* ---------------- 优惠券 ---------------- */
    // GET /voucher/list/{shopId}
    vouchersOfShop: function (shopId) {
      return data(http.get('/voucher/list/' + shopId));
    },
    // POST /voucher-order/seckill/{id}
    seckill: function (id) {
      return data(http.post('/voucher-order/seckill/' + id));
    },

    /* ---------------- 用户 ---------------- */
    // POST /user/code?phone=
    sendCode: function (phone) {
      return data(http.post('/user/code', null, { params: { phone: phone } }));
    },
    // POST /user/login
    login: function (form) {
      return data(http.post('/user/login', form));
    },
    // POST /user/logout
    logout: function () {
      return data(http.post('/user/logout'));
    },
    // GET /user/me
    me: function () {
      return data(http.get('/user/me'));
    },
    // GET /user/{id}
    user: function (id) {
      return data(http.get('/user/' + id));
    },
    // GET /user/info/{id}
    userInfo: function (id) {
      return data(http.get('/user/info/' + id));
    },

    /* ---------------- 关注 ---------------- */
    // PUT /follow/{followUserId}/{isFollow}
    follow: function (id, isFollow) {
      return data(http.put('/follow/' + id + '/' + isFollow));
    },
    // GET /follow/or/not/{followUserId}
    isFollow: function (id) {
      return data(http.get('/follow/or/not/' + id));
    },
    // GET /follow/common/{followUserId}
    commonFollow: function (id) {
      return data(http.get('/follow/common/' + id));
    }
  };

  global.api = api;
  global.TOKEN_KEY = TOKEN_KEY;
})(window);
