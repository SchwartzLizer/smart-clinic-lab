module.exports = {
  env: {
    browser: true,
    es2021: true
  },
  parserOptions: {
    ecmaVersion: "latest",
    sourceType: "module"
  },
  rules: {
    // Browser pages share functions through script tags; runtime globals are
    // intentionally not treated as per-file JavaScript errors during migration.
    "no-undef": "off",
    "no-unused-vars": "off"
  }
};
