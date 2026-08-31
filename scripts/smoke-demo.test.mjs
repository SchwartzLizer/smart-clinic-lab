import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { readFileSync } from 'node:fs';
import test from 'node:test';

import { SmokeError, createSafeLookup, isPublicAddress, readConfig, requestJson, runSmoke } from './smoke-demo.mjs';

const environment = {
  SMOKE_BASE_URL: 'https://demo.example.test/',
  EXPECTED_REVISION: '0aab16ba29582a5aaba5a984e2ded38f30576e0b',
  EXPECTED_VERSION: '0.0.1-SNAPSHOT',
  EXPECTED_ENVIRONMENT: 'cloud'
};

function publicLookup(_hostname, _options, callback) { callback(null, [{ address: '93.184.216.34', family: 4 }]); }
function jsonBody(body) { return Buffer.from(JSON.stringify(body)); }
function response({ statusCode = 200, body, headers = { 'content-type': 'application/json' }, beforeEnd, neverEnd = false } = {}) {
  const stream = new EventEmitter();
  stream.statusCode = statusCode;
  stream.headers = headers;
  stream.destroyed = false;
  stream.destroy = () => { stream.destroyed = true; };
  stream.resume = () => {};
  stream.emitBody = () => {
    if (neverEnd) return;
    for (const chunk of Array.isArray(body) ? body : [body]) stream.emit('data', chunk);
    beforeEnd?.();
    stream.emit('end');
  };
  return stream;
}
function requestFactory(responses, captures = []) {
  return (url, options, callback) => {
    const request = new EventEmitter();
    request.setTimeout = (milliseconds, handler) => { request.timeout = milliseconds; request.timeoutHandler = handler; };
    request.destroyed = false;
    request.destroy = (error) => { request.destroyed = true; request.emit('error', error); };
    request.end = () => {
      captures.push({ url, options, request });
      options.lookup(new URL(url).hostname, {}, (lookupError) => {
        if (lookupError) return request.emit('error', lookupError);
        const next = responses.shift();
        if (next instanceof Error) return request.emit('error', next);
        callback(next);
        next.emitBody();
      });
    };
    return request;
  };
}
function passingResponses() {
  return [
    response({ body: jsonBody({ status: 'UP' }) }),
    response({ body: jsonBody({ status: 'UP' }) }),
    response({ body: jsonBody({ app: { revision: environment.EXPECTED_REVISION, version: environment.EXPECTED_VERSION, environment: 'cloud', data: 'synthetic', sla: 'none' } }) })
  ];
}

test('readConfig rejects unsafe URL and mapped IPv6 forms', () => {
  assert.equal(readConfig(environment).baseUrl.toString(), 'https://demo.example.test/');
  for (const unsafe of ['http://demo.example.test/', 'https://user:password@demo.example.test/', 'https://demo.example.test/path', 'https://localhost/', 'https://127.0.0.1/', 'https://[::1]/', 'https://[::ffff:127.0.0.1]/', 'https://[::ffff:7f00:1]/']) assert.throws(() => readConfig({ ...environment, SMOKE_BASE_URL: unsafe }), SmokeError);
});

test('isPublicAddress rejects private, loopback, link-local, multicast, reserved, mapped, and IANA special IPv6 ranges', () => {
  for (const address of ['0.0.0.0', '10.1.2.3', '100.64.0.1', '127.0.0.1', '169.254.1.1', '172.16.0.1', '192.168.1.1', '198.18.0.1', '224.0.0.1', '240.0.0.1', '::', '::1', '::ffff:127.0.0.1', '::ffff:7f00:1', '64:ff9b::7f00:1', '64:ff9b:1::7f00:1', '100::1', '100:0:0:1::1', '2001::1', '2001:5::1', '2001:db8::1', '2002:c000:201::1', '3ffe::1', '3fff::1', '4000::1', '5f00::1', '8000::1', 'fc00::1', 'fe80::1', 'fec0::1', 'ff02::1']) assert.equal(isPublicAddress(address), false, address);
  assert.equal(isPublicAddress('93.184.216.34'), true);
  assert.equal(isPublicAddress('2001:4860:4860::8888'), true);
  assert.equal(isPublicAddress('2606:2800:220:1:248:1893:25c8:1946'), true);
});

test('connection-time lookup rejects private/mixed answers and permits public answer', async () => {
  const rejectLookup = (addresses) => new Promise((resolve) => createSafeLookup((_host, _options, callback) => callback(null, addresses))('demo.example.test', {}, (error) => { assert.match(error.message, /only public/); resolve(); }));
  await rejectLookup([{ address: '127.0.0.1', family: 4 }]);
  await rejectLookup([{ address: '93.184.216.34', family: 4 }, { address: '10.0.0.1', family: 4 }]);
  await new Promise((resolve, reject) => createSafeLookup(publicLookup)('demo.example.test', {}, (error, address, family) => error ? reject(error) : (assert.equal(address, '93.184.216.34'), assert.equal(family, 4), resolve())));
});

test('runSmoke verifies endpoints without request credentials or raw URL logging', async () => {
  const captures = [];
  const logs = [];
  await runSmoke({ config: readConfig(environment), requestFn: requestFactory(passingResponses(), captures), lookupFn: publicLookup, logger: { log: (message) => logs.push(message) } });
  assert.deepEqual(captures.map(({ url }) => new URL(url).pathname), ['/actuator/health/liveness', '/actuator/health/readiness', '/actuator/info']);
  assert.equal(captures.every(({ options }) => Object.keys(options.headers).length === 0 && typeof options.lookup === 'function'), true);
  assert.equal(logs.some((message) => message.includes('demo.example.test')), false);
});

test('streamed response cap destroys stream before buffering more than 16 KiB', async () => {
  const oversized = response({ body: [Buffer.alloc(16 * 1024), Buffer.alloc(1)] });
  await assert.rejects(requestJson({ url: 'https://demo.example.test/actuator/info', label: 'Info', requestFn: requestFactory([oversized]), lookupFn: publicLookup, deadline: Date.now() + 10_000 }), /exceeded safe size/);
  assert.equal(oversized.destroyed, true);
});

test('response stream errors do not expose transport details', async () => {
  const requestFn = (url, options, callback) => {
    const request = new EventEmitter();
    request.setTimeout = () => {};
    request.destroy = (error) => request.emit('error', error);
    request.end = () => options.lookup(new URL(url).hostname, {}, () => {
      const stream = response({ body: Buffer.alloc(0) });
      callback(stream);
      stream.emit('error', new Error('untrusted address detail'));
    });
    return request;
  };
  await assert.rejects(requestJson({ url: 'https://demo.example.test/actuator/info', label: 'Info', requestFn, lookupFn: publicLookup, deadline: Date.now() + 10_000 }), /response stream failure/);
});

test('absolute timer rejects hanging lookup/connect and clears timer on every settle path', async () => {
  const timers = [];
  const setTimer = (callback, delay) => {
    const handle = { callback, delay, cleared: false };
    timers.push(handle);
    return handle;
  };
  const clearTimer = (handle) => { handle.cleared = true; };
  for (const mode of ['lookup', 'connect']) {
    const requestFn = (_url, options) => {
      const request = new EventEmitter();
      request.setTimeout = () => {};
      request.destroy = (error) => request.emit('error', error);
      request.end = () => {
        if (mode === 'lookup') return;
        options.lookup('demo.example.test', {}, () => {});
      };
      return request;
    };
    const pending = requestJson({ url: 'https://demo.example.test/actuator/info', label: 'Info', requestFn, lookupFn: publicLookup, now: () => 0, deadline: 5, setTimer, clearTimer });
    const timer = timers.at(-1);
    assert.equal(timer.delay, 5);
    timer.callback();
    await assert.rejects(pending, /exceeded its global deadline/);
    assert.equal(timer.cleared, true);
  }
  const successTimers = [];
  await requestJson({ url: 'https://demo.example.test/actuator/info', label: 'Info', requestFn: requestFactory([response({ body: jsonBody({}) })]), lookupFn: publicLookup, deadline: Date.now() + 10_000, setTimer: (callback, delay) => {
    const handle = { callback, delay, cleared: false };
    successTimers.push(handle);
    return handle;
  }, clearTimer });
  assert.equal(successTimers[0].cleared, true);
});

test('early status and header rejection destroys never-ending response/request and clears timer', async () => {
  const cases = [
    response({ statusCode: 503, neverEnd: true }),
    response({ headers: { 'content-type': 'text/plain' }, neverEnd: true }),
    response({ headers: { 'content-type': 'application/json', 'content-length': String(16 * 1024 + 1) }, neverEnd: true })
  ];
  for (const stream of cases) {
    const captures = [];
    const timers = [];
    const pending = requestJson({
      url: 'https://demo.example.test/actuator/info', label: 'Info', requestFn: requestFactory([stream], captures), lookupFn: publicLookup,
      deadline: Date.now() + 10_000,
      setTimer: (callback, delay) => { const handle = { callback, delay, cleared: false }; timers.push(handle); return handle; },
      clearTimer: (handle) => { handle.cleared = true; }
    });
    await assert.rejects(pending);
    assert.equal(stream.destroyed, true);
    assert.equal(captures[0].request.destroyed, true);
    assert.equal(timers[0].cleared, true);
  }
});

test('absolute timer destroys active never-ending response and request before direct rejection', async () => {
  const stream = response({ neverEnd: true });
  const captures = [];
  let timer;
  const pending = requestJson({
    url: 'https://demo.example.test/actuator/info', label: 'Info', requestFn: requestFactory([stream], captures), lookupFn: publicLookup, now: () => 0, deadline: 5,
    setTimer: (callback, delay) => { timer = { callback, delay, cleared: false }; return timer; }, clearTimer: (handle) => { handle.cleared = true; }
  });
  timer.callback();
  await assert.rejects(pending, /exceeded its global deadline/);
  assert.equal(stream.destroyed, true);
  assert.equal(captures[0].request.destroyed, true);
  assert.equal(timer.cleared, true);
});

test('runSmoke retries transient readiness and stale revision only', async () => {
  const stale = response({ body: jsonBody({ app: { revision: 'stale', version: environment.EXPECTED_VERSION, environment: 'cloud', data: 'synthetic', sla: 'none' } }) });
  const responses = [response({ body: jsonBody({ status: 'UP' }) }), response({ body: jsonBody({ status: 'DOWN' }) }), ...passingResponses().slice(0, 2), stale, ...passingResponses()];
  const delays = [];
  await runSmoke({ config: readConfig(environment), requestFn: requestFactory(responses), lookupFn: publicLookup, sleep: async (delay) => delays.push(delay), logger: { log() {} } });
  assert.deepEqual(delays, [1_000, 2_000]);
});

test('global deadline fails slow successful sequence and request timeout honors remaining budget', async () => {
  let clock = 0;
  const slow = response({ body: jsonBody({ status: 'UP' }), beforeEnd: () => { clock = 360_000; } });
  await assert.rejects(runSmoke({ config: readConfig(environment), requestFn: requestFactory([slow]), lookupFn: publicLookup, now: () => clock, logger: { log() {} } }), /global deadline/);
  const captures = [];
  clock = 5_500;
  await requestJson({ url: 'https://demo.example.test/actuator/info', label: 'Info', requestFn: requestFactory([response({ body: jsonBody({}) })], captures), lookupFn: publicLookup, now: () => clock, deadline: 10_000 });
  assert.equal(captures[0].request.timeout, 4_500);
});

test('retry sleep cannot overshoot global deadline', async () => {
  let clock = 0;
  const delays = [];
  const baseRequest = requestFactory([response({ statusCode: 503, body: Buffer.alloc(0) })]);
  const requestFn = (...args) => { clock = 359_500; return baseRequest(...args); };
  await assert.rejects(runSmoke({ config: readConfig(environment), requestFn, lookupFn: publicLookup, now: () => clock, sleep: async (delay) => { delays.push(delay); clock += delay; }, logger: { log() {} } }), /global deadline/);
  assert.deepEqual(delays, [500]);
});

test('unsafe metadata and non-cloud expectation fail without retries', async () => {
  await assert.rejects(runSmoke({ config: readConfig(environment), requestFn: requestFactory([response({ body: jsonBody({ status: 'UP', token: 'not-logged' }) })]), lookupFn: publicLookup, logger: { log() {} } }), /unexpected sensitive field/);
  assert.throws(() => readConfig({ ...environment, EXPECTED_ENVIRONMENT: 'local' }), /must be cloud/);
});

test('release workflow binds smoke identity to candidate outputs', () => {
  const workflow = readFileSync(new URL('../.github/workflows/phase3-release-readiness.yml', import.meta.url), 'utf8');
  assert.match(workflow, /source_commit:\s*\$\{\{\s*steps\.candidate\.outputs\.source_commit\s*}}/);
  assert.match(workflow, /expected_app_version:\s*\$\{\{\s*steps\.candidate\.outputs\.expected_app_version\s*}}/);
  assert.match(workflow, /EXPECTED_REVISION:\s*\$\{\{\s*needs\.release-candidate\.outputs\.source_commit\s*}}/);
  assert.match(workflow, /EXPECTED_VERSION:\s*\$\{\{\s*needs\.release-candidate\.outputs\.expected_app_version\s*}}/);
  assert.match(workflow, /EXPECTED_ENVIRONMENT:\s*cloud/);
  assert.doesNotMatch(workflow, /^\s+EXPECTED_REVISION:\s*\n\s+description:/m);
  assert.doesNotMatch(workflow, /^\s+EXPECTED_VERSION:\s*\n\s+description:/m);
});
