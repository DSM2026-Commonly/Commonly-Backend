#!/usr/bin/env bash
#
# Commonly-be DB 초기화 + 담당자 계정 생성
#
#   ⚠️  모든 데이터를 지운다. 되돌릴 수 없다.
#
# 사용법:
#   scripts/init-db.sh                 # 확인 프롬프트 후 초기화 + 계정 생성
#   scripts/init-db.sh --seed-only     # 스키마는 건드리지 않고 계정만 생성
#   scripts/init-db.sh --yes           # 프롬프트 없이 (CI/스크립트용)
#
# 저장소 루트에서 실행한다. docker compose로 띄운 postgres를 대상으로 한다.
#
set -euo pipefail

cd "$(dirname "$0")/.."

SEED_ONLY=false
ASSUME_YES=false
for arg in "$@"; do
  case "$arg" in
    --seed-only) SEED_ONLY=true ;;
    --yes|-y)    ASSUME_YES=true ;;
    *) echo "알 수 없는 옵션: $arg" >&2; exit 2 ;;
  esac
done

if [[ ! -f .env ]]; then
  echo "오류: .env가 없다. docker-compose가 읽는 파일이다." >&2
  exit 1
fi

# POSTGRES_* 만 가져온다. 다른 값(JWT_SECRET 등)을 셸에 풀지 않는다.
POSTGRES_DB=$(grep -E '^POSTGRES_DB=' .env | tail -1 | cut -d= -f2-)
POSTGRES_USER=$(grep -E '^POSTGRES_USER=' .env | tail -1 | cut -d= -f2-)
if [[ -z "${POSTGRES_DB}" || -z "${POSTGRES_USER}" ]]; then
  echo "오류: .env에서 POSTGRES_DB / POSTGRES_USER를 찾을 수 없다." >&2
  exit 1
fi

COMPOSE="docker compose"
$COMPOSE version >/dev/null 2>&1 || COMPOSE="docker-compose"

psql_file() {
  $COMPOSE exec -T postgres \
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -q -f - < "$1"
}

if [[ "$SEED_ONLY" == false ]]; then
  echo "대상 : ${POSTGRES_USER}@${POSTGRES_DB} (docker compose postgres)"
  echo ""
  echo "  ⚠️  모든 테이블을 DROP하고 다시 만든다. 데이터가 전부 사라진다."
  echo "      발급된 증명서 기록도 지워진다. S3(Garage)에 올라간 PDF 파일은 남지만"
  echo "      가리키는 DB 행이 없어져 접근할 수 없게 된다."
  echo ""
  if [[ "$ASSUME_YES" == false ]]; then
    read -r -p "정말 진행하려면 'RESET'을 입력: " confirm
    [[ "$confirm" == "RESET" ]] || { echo "취소했다."; exit 1; }
  fi

  # 앱이 떠 있는 상태로 테이블을 지우면 진행 중인 요청이 깨진다.
  echo "==> 앱 중단"
  $COMPOSE stop app || true

  echo "==> 스키마 초기화"
  psql_file scripts/init-db.sql
fi

echo "==> 담당자 계정 생성"
psql_file scripts/seed-admin.sql

if [[ "$SEED_ONLY" == false ]]; then
  echo "==> 앱 재시작"
  $COMPOSE start app
  echo ""
  echo "기동 확인:  $COMPOSE logs -f app"
fi

echo ""
echo "완료. 로그인:"
echo "  아이디   abcd1234"
echo "  비밀번호 abcd1234!"
echo ""
echo "  curl -s https://commonly-be.iswebj.kr/api/auths/login \\"
echo "    -H 'content-type: application/json' \\"
echo "    -d '{\"accountId\":\"abcd1234\",\"password\":\"abcd1234!\"}'"
