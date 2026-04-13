#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export NODE_HOME="/opt/homebrew/opt/node@22"
export PATH="$JAVA_HOME/bin:$NODE_HOME/bin:$PATH"

if [ ! -x "$JAVA_HOME/bin/javac" ]; then
  echo "JDK 17 not found at: $JAVA_HOME" >&2
  echo "Install it with: /opt/homebrew/bin/brew install openjdk@17" >&2
  exit 1
fi

if [ ! -x "$NODE_HOME/bin/node" ]; then
  echo "Node 22 not found at: $NODE_HOME" >&2
  echo "Install it with: /opt/homebrew/bin/brew install node@22" >&2
  exit 1
fi

echo "Using Node: $(node --version)"
echo "Using Java: $(java -version 2>&1 | head -n 1)"

if [ ! -d node_modules ]; then
  npm ci
fi

chmod +x ./mvnw

npm run compile

if curl -sS -X POST \
  -H 'Content-Type: application/json' \
  --data '{"jsonrpc":"2.0","method":"web3_clientVersion","params":[],"id":1}' \
  http://127.0.0.1:8545 >/dev/null 2>&1; then
  echo "Port 8545 already has an RPC server. Stop it first, then rerun this script." >&2
  echo "On macOS you can inspect it with: lsof -nP -iTCP:8545 -sTCP:LISTEN" >&2
  exit 1
fi

: > hardhat-node.log
npm run node > hardhat-node.log 2>&1 &
NODE_PID=$!

cleanup() {
  kill "$NODE_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Waiting for Hardhat node on http://127.0.0.1:8545 ..."
NODE_READY=false
for _ in $(seq 1 30); do
  if ! kill -0 "$NODE_PID" >/dev/null 2>&1; then
    echo "Hardhat node exited before it became ready. Log:" >&2
    tail -80 hardhat-node.log >&2
    exit 1
  fi

  if curl -sS -X POST \
    -H 'Content-Type: application/json' \
    --data '{"jsonrpc":"2.0","method":"web3_clientVersion","params":[],"id":1}' \
    http://127.0.0.1:8545 >/dev/null 2>&1; then
    NODE_READY=true
    break
  fi
  sleep 1
done

if [ "$NODE_READY" != "true" ]; then
  echo "Hardhat node did not become ready within 30 seconds. Log:" >&2
  tail -80 hardhat-node.log >&2
  exit 1
fi

npm run -s deploy:raw
./mvnw test
