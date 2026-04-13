#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export NODE_HOME="${NODE_HOME:-/opt/homebrew/opt/node@22}"
export PATH="$JAVA_HOME/bin:$NODE_HOME/bin:$PATH"

if [ ! -x "$JAVA_HOME/bin/javac" ]; then
  echo "JDK 17 not found at: $JAVA_HOME" >&2
  exit 1
fi
if [ ! -x "$NODE_HOME/bin/node" ]; then
  echo "Node not found at: $NODE_HOME" >&2
  exit 1
fi

if [ ! -d node_modules ]; then
  npm ci
fi

chmod +x ./mvnw
npm run compile

# Start Hardhat if nothing is listening on 8545.
START_NODE=true
if curl -sS -X POST -H 'Content-Type: application/json' \
  --data '{"jsonrpc":"2.0","method":"web3_clientVersion","params":[],"id":1}' \
  http://127.0.0.1:8545 >/dev/null 2>&1; then
  START_NODE=false
  echo "Reusing existing RPC server on 8545."
fi

NODE_PID=""
if [ "$START_NODE" = "true" ]; then
  : > hardhat-node.log
  npm run node > hardhat-node.log 2>&1 &
  NODE_PID=$!
  trap '[ -n "$NODE_PID" ] && kill "$NODE_PID" >/dev/null 2>&1 || true' EXIT

  echo "Waiting for Hardhat on http://127.0.0.1:8545 ..."
  for _ in $(seq 1 30); do
    if curl -sS -X POST -H 'Content-Type: application/json' \
      --data '{"jsonrpc":"2.0","method":"web3_clientVersion","params":[],"id":1}' \
      http://127.0.0.1:8545 >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done
fi

# Deploy fresh contract + seed treasury. The first deployment on a clean chain
# yields a deterministic address that matches the BlockchainService default.
npm run -s deploy:raw

echo ""
echo "============================================================"
echo " Frontend:  http://localhost:8080"
echo " REST api:  http://localhost:8080/api/state"
echo "============================================================"
echo ""

./mvnw -q spring-boot:run
