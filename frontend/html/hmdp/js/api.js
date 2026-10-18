/*
 * 黑马点评 · API 访问层
 * ---------------------------------------------------------------
 * 本文件是前端访问后端的唯一入口，所有请求路径、请求方式、
 * 查询参数均与重写前页面中的调用保持一一对应，不做任何合并、
 * 改名或新增。
 *
 * 响应约定：
 *   { success: Boolean, errorMsg: String, data: Any, total: Long }
 *   - 成功时 resolve 后端 data 字段
 *   - 失败时 reject errorMsg（或「请先登录」「服务器异常」）
 *   - HTTP 401 时跳转登录页，与旧版拦截器行为一致
 */
(function (global) {
  'use strict';

  var BASE_URL = '/api';
  var TOKEN_KEY = 'token';

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
        return Promise.reject((body && body.errorMsg) || '请求失败');
      }
      return body;
    },
    function (error) {
      if (error.response && error.response.status === 401) {
        setTimeout(function () {
          location.href = '/login.html';
        }, 200);
        return Promise.reject('请先登录');
      }
      return Promise.reject('服务器异常');
    }
  );

  function data(promise) {
    return promise.then(function (res) {
      return res.data;
    });
  }

  var api = {
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
