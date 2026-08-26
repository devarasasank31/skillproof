#!/usr/bin/env bash
# Daily production health check for SkillProof.
# Exits non-zero if any core flow fails, so GitHub Actions flags it.
set -uo pipefail

API="https://skillprooff.onrender.com"
APP="https://skillproof-bice.vercel.app"
PASS=0
FAIL=0

check() {
  local name="$1" ok="$2" detail="$3"
  if [ "$ok" = "true" ]; then
    echo "PASS  $name  ($detail)"
    PASS=$((PASS+1))
  else
    echo "FAIL  $name  ($detail)"
    FAIL=$((FAIL+1))
  fi
}

code() { curl -sL -o /dev/null -w "%{http_code}" "$@"; }

echo "== 1. Backend & frontend reachable =="
# Render free tier sleeps; first hit may need a long cold start. Retry a few times.
backend=""
for attempt in 1 2 3; do
  backend=$(code --max-time 240 "$API/swagger-ui.html")
  [ "$backend" = "200" ] && break
  echo "  attempt $attempt got $backend, waiting 30s before retry..."
  sleep 30
done
check "backend-up" "$([ "$backend" = "200" ] && echo true || echo false)" "HTTP $backend"
fw=$(code --max-time 30 "$APP/login")
check "frontend-up" "$([ "$fw" = "200" ] && echo true || echo false)" "HTTP $fw"

echo "== 2. Register + verification gate =="
EMAIL="diag-$(date +%s)-$RANDOM@test.com"
rc=$(code -X POST --max-time 60 -H "Content-Type: application/json" \
  -d "{\"name\":\"Health Bot\",\"email\":\"$EMAIL\",\"password\":\"HealthCheck1!\"}" \
  "$API/api/auth/register")
check "register-fast" "$([ "$rc" = "201" ] && echo true || echo false)" "HTTP $rc"

# Unverified accounts must be rejected with EMAIL_NOT_VERIFIED (HTTP 403).
gate_code=$(code -X POST --max-time 30 -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"HealthCheck1!\"}" \
  "$API/api/auth/login")
gate_body=$(curl -s -X POST --max-time 30 -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"HealthCheck1!\"}" \
  "$API/api/auth/login")
case "$gate_body" in
  *EMAIL_NOT_VERIFIED*) check "email-gate" "true" "403 EMAIL_NOT_VERIFIED (got $gate_code)" ;;
  *) check "email-gate" "false" "body: ${gate_body:0:120}" ;;
esac

echo "== 3. Demo login + core flows =="
login=$(curl -s -X POST --max-time 30 -H "Content-Type: application/json" \
  -d '{"email":"demo@skillproof.dev","password":"Demo1234!"}' \
  "$API/api/auth/login")
TOKEN=$(echo "$login" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
check "demo-login" "$([ -n "$TOKEN" ] && echo true || echo false)" "token $([ -n "$TOKEN" ] && echo ok || echo missing)"
AUTH="Authorization: Bearer $TOKEN"

skills=$(curl -s -m 30 -H "$AUTH" "$API/api/skills")
skill_count=$(echo "$skills" | grep -o '"id":' | wc -l)
check "skills-list" "$([ "$skill_count" -gt 0 ] && echo true || echo false)" "$skill_count skills"
USER_SKILL_ID=$(echo "$skills" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

assess=$(curl -s -m 90 -X POST -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"count":15}' "$API/api/skills/$USER_SKILL_ID/assess")
q_count=$(echo "$assess" | grep -o '"prompt"' | wc -l)
check "assessment-15" "$([ "$q_count" -ge 12 ] && echo true || echo false)" "requested 15 got $q_count"

ASSESS_ID=$(echo "$assess" | grep -o '"assessmentId":[0-9]*' | head -1 | cut -d: -f2)
QID=$(echo "$assess" | grep -o '"id":[0-9]*' | sed -n 2p | cut -d: -f2)
answer=$(curl -s -m 60 -X POST -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"questionId\":$QID,\"answerText\":\"A thorough sample answer discussing the core concept with concrete examples and trade-offs.\"}" \
  "$API/api/assessments/$ASSESS_ID/answers")
case "$answer" in
  *answerKey*|*explanation*) check "answer-reveal" "true" "reveal present" ;;
  *) check "answer-reveal" "false" "no answerKey/explanation in response" ;;
esac
cc=$(code -X POST --max-time 60 -H "$AUTH" "$API/api/assessments/$ASSESS_ID/complete")
check "assessment-complete" "$([ "$cc" = "200" ] && echo true || echo false)" "HTTP $cc"

challenges=$(curl -s -m 30 -H "$AUTH" "$API/api/challenges")
ch_count=$(echo "$challenges" | grep -o '"title"' | wc -l)
check "challenges-scoped" "$([ "$ch_count" -ge 0 ] && echo true || echo false)" "HTTP 200, $ch_count items"

ids=$(echo "$skills" | grep -o '"skillId":[0-9]*' | head -3 | cut -d: -f2 | paste -sd, -)
interview=$(curl -s -m 120 -X POST -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"targetRole\":\"Health Check\",\"skillIds\":[$ids]}" \
  "$API/api/interviews")
iv_count=$(echo "$interview" | grep -o '"prompt"' | wc -l)
check "interview-start" "$([ "$iv_count" -gt 0 ] && echo true || echo false)" "$iv_count questions"

for ep in dashboard analytics jobs profile/ai; do
  c=$(code --max-time 30 -H "$AUTH" "$API/api/$ep")
  check "endpoint-$ep" "$([ "$c" = "200" ] && echo true || echo false)" "HTTP $c"
done

echo ""
echo "== RESULT: $PASS passed, $FAIL failed =="
[ "$FAIL" -eq 0 ]

