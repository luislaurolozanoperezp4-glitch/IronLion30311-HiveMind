#!/usr/bin/env bash
# Grep-based checks for the project rules in CLAUDE.md and docs/ARCHITECTURE.md.
# Runs in CI (job "code-rules") and locally: bash .github/scripts/check-code-rules.sh
# Exits 1 and lists every problem it finds; exits 0 when the code follows the rules.
set -u

cd "$(dirname "$0")/../.." || exit 2

TEAMCODE="TeamCode/src"
MAIN="$TEAMCODE/main/java/org/firstinspires/ftc/teamcode"
failures=0

fail() {
    echo "::error::$1"
    failures=$((failures + 1))
}

# 1. No sleep() in TeleOp, AUTO, subsystems or state machines (ARCHITECTURE rule 2).
#    The Pedro tuning OpModes in pathing/tuning are upstream code and may block.
sleep_hits=$(grep -rnE '(^|[^A-Za-z_.])(Thread\.)?sleep\(' \
    "$MAIN/opmodes" "$MAIN/subsystems" "$MAIN/statemachine" "$MAIN/util" "$MAIN/hardware" \
    --include='*.java' | grep -vE '^[^:]+:[0-9]+:\s*(//|\*|/\*)' || true)
if [ -n "$sleep_hits" ]; then
    fail "sleep() found (use ElapsedTime / state machines instead):"
    echo "$sleep_hits"
fi

# 2. Every Java file starts with a "// Sources:" line (CLAUDE.md).
for f in $(find "$TEAMCODE" -name '*.java'); do
    if ! head -n 1 "$f" | grep -q '^// Sources:'; then
        fail "$f does not start with '// Sources: ...'"
    fi
done

# 3. No version or leftover names in file names (v2, _old, Final, Copy, Test2, Backup).
bad_names=$(find "$TEAMCODE" -name '*.java' | grep -E '(V[0-9]+|v[0-9]+|_old|Old|Final|Copy|Backup|Test[0-9]+)\.java$' || true)
if [ -n "$bad_names" ]; then
    fail "file names with version/leftover suffixes:"
    echo "$bad_names"
fi

# 4. Everything in English: common Spanish words must not appear in TeamCode code or comments.
#    "// Sources:" lines are skipped because they quote the old repo name.
spanish='\b(izquierd[oa]s?|derech[oa]s?|adelante|atras|atrás|girar|disparar|disparador|lanzador|brazo|garra|pelotas?|bolas?|tiempo|distancia|velocidad|potencia|autonomo|autónomo|recolector|recoger|motores|camara|cámara|esperar|avanzar|rueda|ruedas|tirar|también|porque|cuando|aqui|aquí|ahora|entonces|mientras)\b'
spanish_hits=$(grep -rniE "$spanish" "$TEAMCODE" --include='*.java' | grep -v '// Sources:\|//          ' || true)
if [ -n "$spanish_hits" ]; then
    fail "Spanish words found (everything must be in English):"
    echo "$spanish_hits"
fi

# 5. OpMode groups are only Competition or Tuning (Dev is allowed for experiments).
group_hits=$(grep -rnE '@(TeleOp|Autonomous)\(' "$MAIN" --include='*.java' \
    | grep -vE 'group = "(Competition|Tuning|Dev)"' || true)
if [ -n "$group_hits" ]; then
    fail "OpMode annotation without group \"Competition\", \"Tuning\" or \"Dev\":"
    echo "$group_hits"
fi

if [ "$failures" -gt 0 ]; then
    echo "Code rules: $failures problem(s) found."
    exit 1
fi
echo "Code rules: OK"
