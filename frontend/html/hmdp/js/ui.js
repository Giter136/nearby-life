/*
 * 黑马点评 · 共享 UI 工具与 Vue 组件
 * 依赖：vue.js、api.js
 */
(function (global) {
  'use strict';

  /* =========================================================
   * 基础工具
   * =======================================================*/
  var util = {
    getUrlParam: function (name) {
      var reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i');
      var r = window.location.search.substr(1).match(reg);
      if (r != null) return decodeURIComponent(r[2]);
      return '';
    },

    // 分 → 元。4750 → 47.50
    formatPrice: function (val) {
      var n = Number(val);
      if (!isFinite(n)) return '0.00';
      return (n / 100).toFixed(2);
    },

    // 折扣：payValue 4750 / actualValue 5000 → 9.5 折
    discount: function (pay, actual) {
      if (!pay || !actual) return '';
      var d = (pay / actual) * 10;
      var text = d.toFixed(1).replace(/\.0$/, '');
      return text + '折';
    },

    // 距离，米
    distance: function (m) {
      if (m === undefined || m === null || m === '') return '';
      var n = Number(m);
      if (!isFinite(n)) return '';
      return n < 1000 ? n.toFixed(0) + ' m' : (n / 1000).toFixed(1) + ' km';
    },

    // 兼容 "2022-03-10 09:21:41" 与 ISO
    parseDate: function (value) {
      if (!value) return null;
      if (value instanceof Date) return value;
      if (typeof value === 'number') return new Date(value);
      var d = new Date(String(value).replace(' ', 'T'));
      return isNaN(d.getTime()) ? null : d;
    },

    formatDate: function (value) {
      var d = util.parseDate(value);
      if (!d) return '';
      return (
        d.getFullYear() +
        '.' +
        String(d.getMonth() + 1).padStart(2, '0') +
        '.' +
        String(d.getDate()).padStart(2, '0')
      );
    },

    formatMonthDay: function (value) {
      var d = util.parseDate(value);
      if (!d) return '';
      return d.getMonth() + 1 + '月' + d.getDate() + '日';
    },

    formatClock: function (value) {
      var d = util.parseDate(value);
      if (!d) return '';
      return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
    },

    // 券有效期：日期区间与时段分两行，便于窄栏排版
    voucherWindow: function (begin, end) {
      var b = util.parseDate(begin);
      var e = util.parseDate(end);
      if (!b || !e) return '';
      var dateLine =
        b.toDateString() === e.toDateString()
          ? util.formatMonthDay(b)
          : util.formatMonthDay(b) + ' — ' + util.formatMonthDay(e);
      return dateLine + '\n' + util.formatClock(b) + ' — ' + util.formatClock(e);
    },

    // 去掉 HTML，用于摘要
    plain: function (html) {
      var box = document.createElement('div');
      box.innerHTML = html || '';
      return (box.textContent || '').replace(/\s+/g, ' ').trim();
    },

    excerpt: function (html, max) {
      var text = util.plain(html);
      max = max || 80;
      if (text.length <= max) return text;
      var cut = text.slice(0, max);
      var lastStop = Math.max(
        cut.lastIndexOf('。'),
        cut.lastIndexOf('！'),
        cut.lastIndexOf('？'),
        cut.lastIndexOf('；')
      );
      if (lastStop >= max * 0.55) return cut.slice(0, lastStop + 1);
      return cut + '…';
    },

    firstImage: function (images) {
      if (!images) return '';
      return String(images).split(',')[0];
    },

    imageList: function (images) {
      return String(images || '')
        .split(',')
        .map(function (s) {
          return s.trim();
        })
        .filter(Boolean);
    },

    today: function () {
      var d = new Date();
      return (
        d.getFullYear() +
        '.' +
        String(d.getMonth() + 1).padStart(2, '0') +
        '.' +
        String(d.getDate()).padStart(2, '0')
      );
    },

    pad: function (n) {
      return String(n).padStart(2, '0');
    },

    coord: function (n) {
      var v = Number(n);
      return isFinite(v) ? v.toFixed(4) : '—';
    },

    loggedIn: function () {
      return !!sessionStorage.getItem(global.TOKEN_KEY || 'token');
    },

    // 清空本地会话（token 失效或主动退出时）
    clearSession: function () {
      sessionStorage.removeItem(global.TOKEN_KEY || 'token');
      sessionStorage.removeItem('userInfo');
      try {
        window.dispatchEvent(new Event('hmdp:session-cleared'));
      } catch (e) {
        /* 老浏览器忽略 */
      }
    },

    // 登录页地址，带“登录后回到哪里”
    loginUrl: function (target) {
      var back = target || (location.pathname + location.search);
      return '/login.html?redirect=' + encodeURIComponent(back);
    },

    // 只允许站内绝对路径，防止开放重定向
    safeRedirect: function (target, fallback) {
      if (typeof target === 'string' && target.charAt(0) === '/' && target.charAt(1) !== '/') {
        return target;
      }
      return fallback || '/index.html';
    },

    goLogin: function (target) {
      location.href = util.loginUrl(target);
    },

    // 后端 tb_user_info.gender：0 男、1 女；接口序列化为 Boolean
    genderText: function (gender) {
      if (gender === true) return '女';
      if (gender === false) return '男';
      return '未填写';
    },

    // 后端 tb_user_info.level 原为 0~9，实体目前映射成 Boolean
    memberText: function (level) {
      if (level === true) return '已开通会员';
      if (level === false) return '普通用户';
      return '未填写';
    },

    isWithin: function (begin, end) {
      var now = Date.now();
      var b = util.parseDate(begin);
      var e = util.parseDate(end);
      if (b && now < b.getTime()) return 'before';
      if (e && now > e.getTime()) return 'after';
      return 'during';
    }
  };
  global.util = util;

  /* =========================================================
   * 轻提示
   * =======================================================*/
  function notify(message, type) {
    if (message && typeof message === 'object' && message.message) {
      message = message.message;
    }
    if (message === null || message === undefined || message === '') {
      message = '操作未完成';
    }
    var host = document.querySelector('.toast-host');
    if (!host) {
      host = document.createElement('div');
      host.className = 'toast-host';
      host.setAttribute('aria-live', 'polite');
      document.body.appendChild(host);
    }
    var item = document.createElement('div');
    item.className = 'toast toast--' + (type || 'info');
    var mark = document.createElement('span');
    mark.className = 'toast__mark';
    mark.textContent = type === 'error' ? '!' : type === 'success' ? '✓' : 'i';
    var text = document.createElement('span');
    text.className = 'toast__text';
    text.textContent = String(message);
    item.appendChild(mark);
    item.appendChild(text);
    host.appendChild(item);
    requestAnimationFrame(function () {
      item.classList.add('is-in');
    });
    var hide = function () {
      item.classList.remove('is-in');
      setTimeout(function () {
        if (item.parentNode) item.parentNode.removeChild(item);
      }, 220);
    };
    var timer = setTimeout(hide, 3200);
    item.addEventListener('click', function () {
      clearTimeout(timer);
      hide();
    });
  }
  global.notify = notify;

  /* =========================================================
   * Vue 指令：滚动入场
   * =======================================================*/
  Vue.directive('reveal', {
    inserted: function (el, binding) {
      var delay = Number(binding.value) || 0;
      el.classList.add('reveal');
      el.style.setProperty('--reveal-delay', delay + 'ms');
      if (!('IntersectionObserver' in window)) {
        el.classList.add('is-in');
        return;
      }
      var io = new IntersectionObserver(
        function (entries) {
          entries.forEach(function (entry) {
            if (entry.isIntersecting) {
              el.classList.add('is-in');
              io.disconnect();
            }
          });
        },
        { rootMargin: '0px 0px -6% 0px', threshold: 0.06 }
      );
      io.observe(el);
    }
  });

  /* =========================================================
   * 组件：图标
   * =======================================================*/
  Vue.component('app-icon', {
    props: { name: { type: String, required: true }, size: { type: [Number, String], default: 16 } },
    template: `<svg class="icon" :width="size" :height="size" aria-hidden="true" focusable="false"><use :href="'/imgs/icon-sprite.svg#i-' + name"></use></svg>`
  });

  /* =========================================================
   * 组件：星级
   * =======================================================*/
  Vue.component('star-rating', {
    props: {
      value: { type: Number, default: 0 },
      showScore: { type: Boolean, default: false },
      size: { type: [Number, String], default: 14 }
    },
    computed: {
      ratio: function () {
        var v = Math.max(0, Math.min(5, Number(this.value) || 0));
        return (v / 5) * 100;
      },
      score: function () {
        return Math.max(0, Math.min(5, Number(this.value) || 0)).toFixed(1);
      }
    },
    template: `<span class="rating">
      <span class="rating__stars" :style="{ '--star-size': size + 'px' }" role="img" :aria-label="'评分 ' + score">
        <span class="rating__layer rating__layer--muted" aria-hidden="true">★★★★★</span>
        <span class="rating__layer rating__layer--lit" :style="{ width: ratio + '%' }" aria-hidden="true">★★★★★</span>
      </span>
      <span v-if="showScore" class="rating__score mono">{{ score }}</span>
    </span>`
  });

  /* =========================================================
   * 组件：悬浮导航
   * =======================================================*/
  Vue.component('site-header', {
    props: {
      active: { type: String, default: '' },
      mode: { type: String, default: 'full' },
      back: { type: String, default: '' }
    },
    data: function () {
      return { scrolled: false, menuOpen: false, logged: util.loggedIn() };
    },
    computed: {
      loginHref: function () {
        return util.loginUrl();
      }
    },
    mounted: function () {
      this.onScroll();
      window.addEventListener('scroll', this.onScroll, { passive: true });
      window.addEventListener('hmdp:session-cleared', this.onSessionCleared);
    },
    beforeDestroy: function () {
      window.removeEventListener('scroll', this.onScroll);
      window.removeEventListener('hmdp:session-cleared', this.onSessionCleared);
    },
    methods: {
      onScroll: function () {
        this.scrolled = (window.scrollY || window.pageYOffset || 0) > 24;
      },
      onSessionCleared: function () {
        this.logged = false;
        this.menuOpen = false;
      },
      closeMenu: function () {
        this.menuOpen = false;
      },
      goBack: function (e) {
        if (window.history.length > 1) {
          e.preventDefault();
          window.history.back();
        }
      }
    },
    template: `<div class="site-top" :class="{ 'is-scrolled': scrolled }">
      <div class="site-top__inner">
        <a class="brand" href="/" aria-label="黑马点评首页">
          <span class="brand__mark"><app-icon name="leaf" :size="26"></app-icon></span>
          <span class="brand__word">黑马点评</span>
        </a>
        <nav v-if="mode === 'full'" class="nav-links" aria-label="主导航">
          <a href="/" :class="{ 'is-active': active === 'home' }">首页</a>
          <a href="/shop-list.html?type=1&name=%E7%BE%8E%E9%A3%9F" :class="{ 'is-active': active === 'shops' }">商户</a>
          <a href="/index.html#hot" :class="{ 'is-active': active === 'hot' }">热门</a>
          <a href="/info.html" :class="{ 'is-active': active === 'profile' }">我的</a>
        </nav>
        <div v-if="mode === 'full'" class="nav-cta">
          <a v-if="!logged" class="btn btn--secondary btn--sm" :href="loginHref">登录</a>
          <a v-else class="btn btn--secondary btn--sm" href="/info.html">个人主页</a>
          <a class="btn btn--primary btn--sm" href="/blog-edit.html">写笔记</a>
          <button class="nav-burger" type="button" :aria-expanded="menuOpen ? 'true' : 'false'" aria-label="打开菜单" @click="menuOpen = !menuOpen">
            <app-icon :name="menuOpen ? 'close' : 'menu'" :size="18"></app-icon>
          </button>
        </div>
        <div v-else class="nav-bare">
          <a class="back-link" :href="back || '/'" @click="!back && goBack($event)">
            <app-icon name="arrow-left" :size="15"></app-icon>返回
          </a>
        </div>
      </div>
      <div v-if="mode === 'full' && menuOpen" class="mobile-sheet">
        <a href="/" @click="closeMenu">首页</a>
        <a href="/shop-list.html?type=1&name=%E7%BE%8E%E9%A3%9F" @click="closeMenu">商户</a>
        <a href="/index.html#hot" @click="closeMenu">热门笔记</a>
        <a href="/info.html" @click="closeMenu">我的主页</a>
        <a v-if="!logged" class="btn btn--secondary btn--sm" :href="loginHref">登录</a>
        <a class="btn btn--primary btn--sm" href="/blog-edit.html">写笔记</a>
      </div>
    </div>`
  });

  /* =========================================================
   * 组件：页脚
   * =======================================================*/
  Vue.component('site-footer', {
    data: function () {
      return { year: new Date().getFullYear() };
    },
    computed: {
      loginHref: function () {
        return util.loginUrl();
      }
    },
    template: `<footer class="site-footer">
      <div class="shell footer-grid">
        <div class="footer-brand">
          <a class="brand brand--footer" href="/">
            <span class="brand__mark"><app-icon name="leaf" :size="28"></app-icon></span>
            <span class="brand__word">黑马点评</span>
          </a>
          <p class="footer-tagline">杭州的城市生活指南。把探店笔记、商户索引和限时优惠，收进同一份城市读本。</p>
          <p class="footer-coord mono">N 30°19′ · E 120°09′</p>
        </div>
        <div class="footer-col">
          <h3>浏览</h3>
          <a href="/">首页</a>
          <a href="/shop-list.html?type=1&name=%E7%BE%8E%E9%A3%9F">美食商户</a>
          <a href="/shop-list.html?type=2&name=KTV">娱乐商户</a>
          <a href="/index.html#hot">热门笔记</a>
        </div>
        <div class="footer-col">
          <h3>账户</h3>
          <a :href="loginHref">登录</a>
          <a href="/info.html">我的主页</a>
          <a href="/blog-edit.html">写笔记</a>
          <a href="/info-edit.html">资料档案</a>
        </div>
        <div class="footer-col">
          <h3>关于</h3>
          <span>黑马点评 · 课程项目</span>
          <span>Spring Boot · Redis · Kafka</span>
          <span>仅供学习演示</span>
        </div>
      </div>
      <div class="shell footer-bottom">
        <span>© {{ year }} hmdp.com</span>
        <span>HANGZHOU CITY GUIDE</span>
      </div>
    </footer>`
  });

  /* =========================================================
   * 组件：笔记卡片
   * =======================================================*/
  Vue.component('note-card', {
    props: {
      note: { type: Object, required: true },
      index: { type: Number, default: 0 },
      variant: { type: String, default: 'grid' },
      delay: { type: Number, default: 0 },
      showAuthor: { type: Boolean, default: true }
    },
    computed: {
      href: function () {
        return '/blog-detail.html?id=' + this.note.id;
      },
      image: function () {
        return util.firstImage(this.note.images);
      },
      excerpt: function () {
        return this.variant === 'row' ? util.excerpt(this.note.content, 76) : '';
      },
      isRow: function () {
        return this.variant === 'row';
      },
      number: function () {
        return this.index ? String(this.index).padStart(2, '0') : '';
      }
    },
    template: `<article :class="isRow ? 'note-row' : 'note-card'" v-reveal="delay">
      <a :class="isRow ? 'note-row__media' : 'note-card__media'" :href="href" :aria-label="note.title">
        <img :src="image" :alt="note.title || '探店笔记'" loading="lazy">
      </a>
      <div :class="isRow ? 'note-row__body' : 'note-card__body'">
        <span :class="isRow ? 'note-row__no' : 'note-card__no'">{{ number }}</span>
        <h3 :class="isRow ? 'note-row__title' : 'note-card__title'"><a :href="href">{{ note.title }}</a></h3>
        <p v-if="isRow && excerpt" class="note-row__excerpt">{{ excerpt }}</p>
        <div :class="isRow ? 'note-row__meta' : 'note-card__meta'">
          <template v-if="showAuthor && note.name">
            <img class="avatar avatar--xs" :src="note.icon || '/imgs/icons/default-icon.png'" alt="">
            <span class="note-card__author">{{ note.name }}</span>
            <span class="meta-dot" aria-hidden="true"></span>
          </template>
          <button class="note-card__like" type="button" :class="{ 'is-liked': note.isLike }" @click.prevent.stop="$emit('like', note)" :aria-label="'点赞' + (note.title || '')">
            <app-icon name="thumb" :size="13"></app-icon><span class="mono">{{ note.liked || 0 }}</span>
          </button>
          <span v-if="isRow" class="note-card__stat"><app-icon name="comment" :size="13"></app-icon><span class="mono">{{ note.comments || 0 }}</span></span>
        </div>
      </div>
    </article>`
  });

  /* =========================================================
   * 组件：空状态 / 登录提示
   * =======================================================*/
  Vue.component('empty-state', {
    props: {
      title: { type: String, default: '这里还没有内容' },
      hint: { type: String, default: '' },
      kicker: { type: String, default: 'CITY GUIDE' }
    },
    template: `<div class="gate">
      <span class="gate__mark"><app-icon name="compass" :size="22"></app-icon></span>
      <p class="eyebrow">{{ kicker }}</p>
      <h2 class="gate__title">{{ title }}</h2>
      <p v-if="hint" class="gate__hint">{{ hint }}</p>
      <div class="gate__actions"><slot></slot></div>
    </div>`
  });

  Vue.component('loading-rule', {
    props: { text: { type: String, default: '正在读取' } },
    template: `<div class="loading" role="status"><span class="loading__ring"></span><span>{{ text }}</span></div>`
  });
})(window);
