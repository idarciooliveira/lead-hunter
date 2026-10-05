#!/usr/bin/env bash
# Lead Hunter CLI eval harness. Deterministic, no LLM, no real Apify calls.
#
# Usage: evals/harness.sh <tag> [company|campaign|run|all]     (default: all)
#
# Builds the lead-hunter:eval Docker image (pom layers cached; only src changes
# rebuild), then runs the CLI container against the scratch Postgres database
# (leadhunter_eval) on the compose network, with a scratch working directory,
# scripted stdin, and transcripts in evals/runs/<tag>-<test>.txt.
#
# Tests:
#   company  - `company setup` create, then `company setup` update (defaults + changes)
#   campaign - `campaign new` wizard, then `campaign run <slug> --dry-run`
#   run      - re-run only the dry-run for an existing campaign
set -uo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
EVALS="$REPO/evals"
TAG="${1:?usage: harness.sh <tag> [company|campaign|run|all]}"
WHAT="${2:-all}"
DB="${LEADHUNTER_EVAL_DB:-leadhunter_eval}"
IMAGE="lead-hunter:eval"
NETWORK="lead-hunter_default"
SLUG="clinicas-dentarias-em-luanda"

COMPOSE="docker compose -f $REPO/docker-compose.yml"

FAILURES=0

note() { printf '%s\n' "$*"; }

build_image() {
  # Always rebuild: agents change the wizards, and a stale image silently evals old code.
  docker build -q -t "$IMAGE" -f "$REPO/backend/Dockerfile" "$REPO/backend" >/dev/null \
    || { note "error: docker build failed"; exit 1; }
}

db_up() {
  $COMPOSE up -d postgres >/dev/null 2>&1 || { note "error: cannot start postgres"; exit 1; }
  docker network inspect "$NETWORK" >/dev/null 2>&1 \
    || { note "error: network $NETWORK missing (run: docker compose up -d postgres once)"; exit 1; }
  $COMPOSE exec -T postgres pg_isready -U leadhunter -d "$DB" >/dev/null 2>&1
  if [ $? -ne 0 ]; then
    $COMPOSE exec -T postgres createdb -U leadhunter "$DB" >/dev/null 2>&1
  fi
}

db_reset() {
  $COMPOSE exec -T postgres psql -U leadhunter -d "$DB" -q \
    -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" \
    || { note "error: cannot reset $DB"; exit 1; }
}

# run_test <name> <answers-file> <args...>
run_test() {
  local name="$1" script="$2"; shift 2
  local out="$EVALS/runs/$TAG-$name.txt"
  mkdir -p "$EVALS/runs"
  {
    cat "$script"
    # Blank-line buffer: trailing confirms/defaults are accepted without a script change
    # when agents A/B/C alter the question flow. Misalignments still surface in the transcript.
    for _ in $(seq 1 60); do echo; done
  } | docker run --rm -i \
        -e PGHOST=postgres -e PGPORT=5432 -e PGDATABASE="$DB" \
        -e PGUSER=leadhunter -e PGPASSWORD=leadhunter \
        --network "$NETWORK" \
        -w /work -v "$WORK:/work" \
        "$IMAGE" "$@" > "$out" 2>&1
  local ec=$?
  note "  $name: exit=$ec -> evals/runs/$TAG-$name.txt"
  if [ "$ec" -ne 0 ]; then FAILURES=$((FAILURES + 1)); fi
}

WORK="$EVALS/work/$TAG"
rm -rf "$WORK"
mkdir -p "$WORK"

note "eval harness tag=$TAG what=$WHAT db=$DB workdir=evals/work/$TAG"
build_image
db_up
if [ "$WHAT" != "run" ]; then
  db_reset
fi

if [ "$WHAT" = "all" ] || [ "$WHAT" = "company" ]; then
  note "test: company"
  run_test company-create "$EVALS/scripts/company-create.txt" company setup
  run_test company-update "$EVALS/scripts/company-update.txt" company setup
fi

if [ "$WHAT" = "all" ] || [ "$WHAT" = "campaign" ]; then
  note "test: campaign"
  run_test campaign-new "$EVALS/scripts/campaign-new.txt" campaign new
  run_test campaign-run-dry /dev/null campaign run "$SLUG" --dry-run
fi

if [ "$WHAT" = "run" ]; then
  note "test: run"
  run_test campaign-run-dry /dev/null campaign run "$SLUG" --dry-run
fi

if [ "$FAILURES" -gt 0 ]; then
  note "harness done: $FAILURES test(s) exited non-zero. Read the transcripts before diagnosing."
  exit 1
fi
note "harness done: all tests exited 0."
