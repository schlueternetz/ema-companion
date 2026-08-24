const chunks = [];
process.stdin.on('data', (d) => chunks.push(d));
process.stdin.on('end', () => {
  let data;
  try {
    data = JSON.parse(Buffer.concat(chunks).toString());
  } catch {
    process.exit(0);
  }

  const cmd = (data.tool_input || {}).command || '';
  if (!/\bgit\s+commit\b/.test(cmd)) process.exit(0);

  const { execSync } = require('child_process');
  let diff;
  try {
    diff = execSync('git diff --cached --unified=0', { encoding: 'utf8' });
  } catch {
    process.exit(0);
  }

  const patterns = [
    [/(?:api[_-]?key|apikey)\s*[:=]\s*['"`]?[A-Za-z0-9_-]{20,}/i, 'API key'],
    [/(?:password|passwd|pwd)\s*[:=]\s*['"`]?\S{8,}/i, 'Password'],
    [/(?:secret[_-]?key|client[_-]?secret)\s*[:=]\s*['"`]?\S{10,}/i, 'Secret key'],
    [/(?:access[_-]?token|auth[_-]?token|bearer\s+[A-Za-z0-9])\s*[:=]\s*['"`]?\S{10,}/i, 'Access token'],
    [/ghp_[A-Za-z0-9]{36}/, 'GitHub personal access token'],
    [/AKIA[0-9A-Z]{16}/, 'AWS access key ID'],
    [/-----BEGIN (?:RSA |EC )?PRIVATE KEY-----/, 'Private key'],
  ];

  const hits = [];
  for (const line of diff.split('\n')) {
    if (!line.startsWith('+') || line.startsWith('+++')) continue;
    for (const [pat, label] of patterns) {
      if (pat.test(line)) {
        hits.push(`${label}: ${line.slice(1, 120).trim()}`);
        break;
      }
    }
  }

  if (hits.length) {
    const reason =
      'Sensitive data detected in staged changes:\n' + hits.map((h) => `  - ${h}`).join('\n');
    process.stdout.write(JSON.stringify({ decision: 'block', reason }));
  } else {
    process.exit(0);
  }
});
