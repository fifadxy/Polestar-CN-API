/*
 * Android probe for the owner's OFFICIAL Polestar China app only.
 * Emits field names / endpoint templates / HTTP status. Never outputs values.
 * No TLS bypass, CAPTCHA bypass, authorization replay or response-body reads.
 */
'use strict';
(function () {
  const PREFIX = 'CN_AUTH_META ';
  const MAX_EVENTS = 320;
  let events = 0;
  const domains = [
    'polestar.cn', 'polestar.com', 'polestar-auto.com',
    'volvocars.com.cn', 'volvocars.com', 'aliyun.com', 'aliyuncs.com'
  ];
  function allowed(host) {
    const h = String(host || '').toLowerCase();
    return domains.some(function (d) { return h === d || h.endsWith('.' + d); });
  }
  function safeName(s) {
    const v = String(s || '');
    return /^[a-zA-Z][a-zA-Z0-9_.-]{0,54}$/.test(v) ? v : ':dynamic';
  }
  function safePath(path) {
    return '/' + String(path || '').split('/').filter(Boolean).map(function (segment) {
      if (/^[0-9]{5,}$/.test(segment) || /^[0-9a-fA-F]{12,}$/.test(segment)
          || segment.length > 55) return ':id';
      return safeName(segment);
    }).join('/');
  }
  function namesOnly(javaSet) {
    if (!javaSet) return [];
    try {
      const it = javaSet.iterator();
      const out = [];
      while (it.hasNext() && out.length < 80) out.push(safeName(it.next()));
      return Array.from(new Set(out)).sort();
    } catch (_) { return []; }
  }
  function fieldNames(value, prefix, depth, out) {
    if (depth > 3 || out.length >= 90) return;
    if (Array.isArray(value)) {
      if (value.length) fieldNames(value[0], prefix + '[]', depth + 1, out);
    } else if (value && typeof value === 'object') {
      Object.keys(value).slice(0, 75).forEach(function (key) {
        if (out.length >= 90) return;
        const k = prefix ? prefix + '.' + safeName(key) : safeName(key);
        out.push(k);
        if (value[key] && typeof value[key] === 'object')
          fieldNames(value[key], k, depth + 1, out);
      });
    }
  }
  function bodyFieldNames(request) {
    try {
      const body = request.body();
      if (!body) return [];
      try { if (body.isDuplex() || body.isOneShot()) return ['<one_shot_body>']; } catch (_) {}
      const length = Number(body.contentLength());
      if (length < 0 || length > 32768) return ['<body_not_read>'];
      const ctype = body.contentType();
      const type = ctype ? String(ctype).toLowerCase() : '';
      if (type.indexOf('json') < 0 && type.indexOf('x-www-form-urlencoded') < 0)
        return ['<other_content_type>'];
      const Buffer = Java.use('okio.Buffer');
      const buffer = Buffer.$new();
      body.writeTo(buffer);
      const raw = String(buffer.readUtf8()); // Local parser only. NEVER printed.
      if (type.indexOf('x-www-form-urlencoded') >= 0) {
        return raw.split('&').slice(0, 90).map(function (pair) {
          let k = pair.split('=')[0].replace(/\+/g, ' ');
          try { k = decodeURIComponent(k); } catch (_) {}
          return safeName(k);
        }).sort();
      }
      const keys = [];
      fieldNames(JSON.parse(raw), '', 0, keys);
      return Array.from(new Set(keys)).sort();
    } catch (_) { return ['<body_names_unavailable>']; }
  }
  function record(request, stage, httpCode) {
    try {
      if (events >= MAX_EVENTS) return;
      const url = request.url(), host = String(url.host()).toLowerCase();
      if (!allowed(host)) return;
      events++;
      const entry = {
        stage: stage,
        method: safeName(request.method()),
        host: host,
        path_template: safePath(url.encodedPath()),
        query_key_names: namesOnly(url.queryParameterNames()),
        header_key_names: namesOnly(request.headers().names()),
        body_key_names: bodyFieldNames(request)
      };
      if (typeof httpCode === 'number' && httpCode >= 100 && httpCode <= 599)
        entry.http_status = httpCode;
      console.log(PREFIX + JSON.stringify(entry));
    } catch (_) {}
  }
  Java.perform(function () {
    let count = 0;
    try {
      const Client = Java.use('okhttp3.OkHttpClient');
      const newCall = Client.newCall.overload('okhttp3.Request');
      newCall.implementation = function (request) {
        record(request, 'initial', null);
        return newCall.call(this, request);
      };
      count++;
    } catch (_) {
      console.log(PREFIX + JSON.stringify({stage:'notice',result:'OkHttp newCall unavailable'}));
    }
    try {
      const Chain = Java.use('okhttp3.internal.http.RealInterceptorChain');
      let found = null;
      Chain.proceed.overloads.forEach(function (item) {
        if (item.argumentTypes.length === 1 &&
            String(item.argumentTypes[0].className) === 'okhttp3.Request') found = item;
      });
      if (found) {
        found.implementation = function (request) {
          const response = found.call(this, request);
          record(request, 'interceptor', Number(response.code()));
          return response;
        };
        count++;
      }
    } catch (_) {
      console.log(PREFIX + JSON.stringify({stage:'notice',result:'OkHttp interceptor unavailable'}));
    }
    console.log(PREFIX + JSON.stringify({
      stage:'ready', package:'com.polestar.china.android',
      installed_hooks:count, mode:'metadata_only', limit:MAX_EVENTS
    }));
  });
})();
