import { lookup as dnsLookup } from 'node:dns';
import https from 'node:https';
import { isIP } from 'node:net';
import { pathToFileURL } from 'node:url';

const MAX_RESPONSE_BYTES = 16 * 1024;
const REQUEST_TIMEOUT_MS = 10_000;
const GLOBAL_DEADLINE_MS = 360_000;
const RETRY_DELAYS_MS = [1_000, 2_000, 5_000, 10_000, 15_000];
const RETRYABLE_STATUS_CODES = new Set([408, 425, 429, 500, 502, 503, 504]);
const SENSITIVE_KEY = /(?:authorization|cookie|credential|password|secret|token)/i;

export class SmokeError extends Error {
  constructor(message, { retryable = false } = {}) {
    super(message);
    this.name = 'SmokeError';
    this.retryable = retryable;
  }
}

function requiredValue(value, name) {
  if (typeof value !== 'string' || value.trim() === '') throw new SmokeError(`Configuration error: ${name} must be set.`);
  if (/[\r\n\0]/.test(value)) throw new SmokeError(`Configuration error: ${name} contains an unsafe character.`);
  return value.trim();
}

function ipv4ToNumber(address) {
  const parts = address.split('.').map(Number);
  if (parts.length !== 4 || parts.some((part) => !Number.isInteger(part) || part < 0 || part > 255)) return null;
  return (((parts[0] << 24) >>> 0) + (parts[1] << 16) + (parts[2] << 8) + parts[3]) >>> 0;
}

function ipv4InCidr(address, network, prefixLength) {
  const value = ipv4ToNumber(address);
  const base = ipv4ToNumber(network);
  if (value === null || base === null) return false;
  const mask = prefixLength === 0 ? 0 : (0xffffffff << (32 - prefixLength)) >>> 0;
  return (value & mask) === (base & mask);
}

function ipv6ToBigInt(address) {
  let normalized = address.toLowerCase().replace(/^\[|\]$/g, '');
  const ipv4Match = normalized.match(/(?:^|:)(\d+\.\d+\.\d+\.\d+)$/);
  if (ipv4Match) {
    const value = ipv4ToNumber(ipv4Match[1]);
    if (value === null) return null;
    normalized = normalized.slice(0, normalized.length - ipv4Match[1].length) + `${(value >>> 16).toString(16)}:${(value & 0xffff).toString(16)}`;
  }
  const hasCompression = normalized.includes('::');
  const [left, right = ''] = normalized.split('::');
  const leftParts = left === '' ? [] : left.split(':');
  const rightParts = right === '' ? [] : right.split(':');
  if (normalized.split('::').length > 2 || leftParts.length + rightParts.length > 8) return null;
  const parts = hasCompression ? [...leftParts, ...Array(8 - leftParts.length - rightParts.length).fill('0'), ...rightParts] : leftParts;
  if (parts.length !== 8 || parts.some((part) => !/^[0-9a-f]{1,4}$/.test(part))) return null;
  return parts.reduce((value, part) => (value << 16n) + BigInt(`0x${part}`), 0n);
}

function ipv6InCidr(address, network, prefixLength) {
  const value = ipv6ToBigInt(address);
  const base = ipv6ToBigInt(network);
  if (value === null || base === null) return false;
  const mask = prefixLength === 0 ? 0n : ((1n << BigInt(prefixLength)) - 1n) << BigInt(128 - prefixLength);
  return (value & mask) === (base & mask);
}

export function isPublicAddress(address) {
  const family = isIP(address);
  if (family === 4) {
    const nonPublicRanges = [['0.0.0.0', 8], ['10.0.0.0', 8], ['100.64.0.0', 10], ['127.0.0.0', 8], ['169.254.0.0', 16], ['172.16.0.0', 12], ['192.0.0.0', 24], ['192.0.2.0', 24], ['192.88.99.0', 24], ['192.168.0.0', 16], ['198.18.0.0', 15], ['198.51.100.0', 24], ['203.0.113.0', 24], ['224.0.0.0', 4], ['240.0.0.0', 4]];
    return !nonPublicRanges.some(([network, prefix]) => ipv4InCidr(address, network, prefix));
  }
  if (family === 6) {
    // Ordinary globally routed IPv6 unicast starts in 2000::/3. Reject every other prefix first.
    if (!ipv6InCidr(address, '2000::', 3)) return false;
    // IANA IPv6 Special-Purpose Address Registry: allow only ordinary global unicast space.
    const nonPublicRanges = [
      ['::', 96], ['::ffff:0:0', 96], ['64:ff9b::', 96], ['64:ff9b:1::', 48],
      ['100::', 64], ['100:0:0:1::', 64], ['2001::', 23], ['2001:db8::', 32], ['2002::', 16],
      ['3ffe::', 16],
      ['3fff::', 20], ['5f00::', 16], ['fc00::', 7], ['fe80::', 10],
      ['fec0::', 10], ['ff00::', 8]
    ];
    return !nonPublicRanges.some(([network, prefix]) => ipv6InCidr(address, network, prefix));
  }
  return false;
}

export function readConfig(environment = process.env) {
  const rawBaseUrl = requiredValue(environment.SMOKE_BASE_URL, 'SMOKE_BASE_URL');
  let baseUrl;
  try { baseUrl = new URL(rawBaseUrl); } catch { throw new SmokeError('Configuration error: SMOKE_BASE_URL must be a valid HTTPS root URL.'); }
  const hostname = baseUrl.hostname.toLowerCase();
  const literalHostname = hostname.replace(/^\[|\]$/g, '');
  if (baseUrl.protocol !== 'https:' || baseUrl.username || baseUrl.password || baseUrl.search || baseUrl.hash || baseUrl.pathname !== '/' || hostname === 'localhost' || hostname.endsWith('.localhost') || (isIP(literalHostname) !== 0 && !isPublicAddress(literalHostname))) {
    throw new SmokeError('Configuration error: SMOKE_BASE_URL must be a public HTTPS origin without credentials, query, fragment, or path.');
  }
  const expectedEnvironment = requiredValue(environment.EXPECTED_ENVIRONMENT ?? 'cloud', 'EXPECTED_ENVIRONMENT');
  if (expectedEnvironment !== 'cloud') throw new SmokeError('Configuration error: EXPECTED_ENVIRONMENT must be cloud.');
  return { baseUrl, expectedRevision: requiredValue(environment.EXPECTED_REVISION, 'EXPECTED_REVISION'), expectedVersion: requiredValue(environment.EXPECTED_VERSION, 'EXPECTED_VERSION'), expectedEnvironment };
}

export function createSafeLookup(lookupFn = dnsLookup) {
  return (hostname, _options, callback) => {
    lookupFn(hostname, { all: true, verbatim: true }, (error, addresses) => {
      if (error) return callback(error);
      if (!Array.isArray(addresses) || addresses.length === 0 || addresses.some(({ address }) => !isPublicAddress(address))) return callback(new SmokeError('DNS resolution did not return only public addresses.'));
      const { address, family } = addresses[0];
      return callback(null, address, family);
    });
  };
}

function assertSafeKeys(value) {
  if (Array.isArray(value)) return value.forEach(assertSafeKeys);
  if (value !== null && typeof value === 'object') for (const [key, child] of Object.entries(value)) {
    if (SENSITIVE_KEY.test(key)) throw new SmokeError('Response contained an unexpected sensitive field.');
    assertSafeKeys(child);
  }
}

function ensureBudget(now, deadline) {
  const remaining = deadline - now();
  if (remaining <= 0) throw new SmokeError('Smoke check exceeded its global deadline.');
  return Math.min(REQUEST_TIMEOUT_MS, remaining);
}

export function requestJson({ url, label, requestFn = https.request, lookupFn = dnsLookup, now = Date.now, deadline, setTimer = setTimeout, clearTimer = clearTimeout }) {
  let requestTimeout;
  try { requestTimeout = ensureBudget(now, deadline); } catch (error) { return Promise.reject(error); }
  return new Promise((resolve, reject) => {
    let settled = false;
    let terminating = false;
    let absoluteTimer;
    let request;
    let activeResponse;
    const finish = (callback, value) => {
      if (!settled) {
        settled = true;
        if (absoluteTimer !== undefined) clearTimer(absoluteTimer);
        callback(value);
      }
    };
    const fail = (error) => finish(reject, error);
    const abort = (response, error) => {
      if (settled || terminating) return;
      terminating = true;
      response?.destroy?.();
      request?.destroy?.();
      fail(error);
    };
    request = requestFn(url, { method: 'GET', headers: {}, lookup: createSafeLookup(lookupFn) }, (response) => {
      activeResponse = response;
      try {
        ensureBudget(now, deadline);
        if (response.statusCode !== 200) {
          return abort(response, new SmokeError(`${label} returned HTTP ${response.statusCode}.`, { retryable: RETRYABLE_STATUS_CODES.has(response.statusCode) }));
        }
        const contentType = response.headers?.['content-type'] ?? '';
        const contentLength = response.headers?.['content-length'];
        if (!/^application\/json(?:\s*;|$)/i.test(contentType)) return abort(response, new SmokeError(`${label} returned a non-JSON response.`));
        if (contentLength !== undefined && Number(contentLength) > MAX_RESPONSE_BYTES) return abort(response, new SmokeError(`${label} response exceeded safe size.`));
        const chunks = [];
        let size = 0;
        response.on('data', (chunk) => {
          try {
            ensureBudget(now, deadline);
            const bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
            if (size + bytes.length > MAX_RESPONSE_BYTES) return abort(response, new SmokeError(`${label} response exceeded safe size.`));
            size += bytes.length;
            chunks.push(bytes);
          } catch (error) { abort(response, error); }
        });
        response.on('error', () => { if (!terminating) abort(response, new SmokeError(`${label} response stream failure.`, { retryable: true })); });
        response.on('end', () => {
          try {
            ensureBudget(now, deadline);
            let json;
            try { json = JSON.parse(Buffer.concat(chunks, size).toString('utf8')); } catch { throw new SmokeError(`${label} returned invalid JSON.`); }
            assertSafeKeys(json);
            finish(resolve, json);
          } catch (error) { abort(response, error); }
        });
      } catch (error) { abort(response, error); }
    });
    if (!settled) {
      absoluteTimer = setTimer(() => abort(activeResponse, new SmokeError(`${label} request exceeded its global deadline.`, { retryable: true })), requestTimeout);
    }
    request.setTimeout?.(requestTimeout, () => request.destroy?.(new SmokeError(`${label} request timed out.`, { retryable: true })));
    request.on('error', (error) => { if (!terminating) fail(error instanceof SmokeError ? error : new SmokeError(`${label} transport failure.`, { retryable: true })); });
    request.end();
  });
}

function endpointUrl(baseUrl, endpoint) { return new URL(endpoint, baseUrl).toString(); }
function checkHealth(json, label, retryable) { if (json?.status !== 'UP') throw new SmokeError(`${label} was not UP.`, { retryable }); }
function checkInfo(json, config) {
  const app = json?.app;
  if (app?.revision !== config.expectedRevision) throw new SmokeError('Info revision did not match expected revision.', { retryable: true });
  if (app?.version !== config.expectedVersion) throw new SmokeError('Info version did not match expected version.');
  if (app?.environment !== config.expectedEnvironment || app?.data !== 'synthetic' || app?.sla !== 'none') throw new SmokeError('Info metadata did not match expected safe markers.');
}

export async function runSmoke({ config = readConfig(), requestFn = https.request, lookupFn = dnsLookup, now = Date.now, sleep = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds)), logger = console } = {}) {
  const deadline = now() + GLOBAL_DEADLINE_MS;
  let attempt = 0;
  while (true) {
    try {
      const liveness = await requestJson({ url: endpointUrl(config.baseUrl, '/actuator/health/liveness'), label: 'Liveness', requestFn, lookupFn, now, deadline });
      checkHealth(liveness, 'Liveness', false);
      const readiness = await requestJson({ url: endpointUrl(config.baseUrl, '/actuator/health/readiness'), label: 'Readiness', requestFn, lookupFn, now, deadline });
      checkHealth(readiness, 'Readiness', true);
      const info = await requestJson({ url: endpointUrl(config.baseUrl, '/actuator/info'), label: 'Info', requestFn, lookupFn, now, deadline });
      checkInfo(info, config);
      ensureBudget(now, deadline);
      logger.log('Smoke check passed: liveness, readiness, and info verified.');
      return;
    } catch (error) {
      if (!(error instanceof SmokeError) || !error.retryable) throw error;
      const remaining = ensureBudget(now, deadline);
      const delay = Math.min(RETRY_DELAYS_MS[Math.min(attempt, RETRY_DELAYS_MS.length - 1)], remaining);
      attempt += 1;
      logger.log(`Smoke check retry ${attempt}: transient condition detected.`);
      await sleep(delay);
      ensureBudget(now, deadline);
    }
  }
}

async function main() {
  try { await runSmoke(); } catch (error) { console.error(`Smoke check failed: ${error instanceof Error ? error.message : 'Unknown smoke checker failure.'}`); process.exitCode = 1; }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) await main();
