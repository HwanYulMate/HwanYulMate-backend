#!/bin/bash

# nginx.conf 생성 스크립트
# nginx.conf.template에서 환경변수를 사용하여 nginx.conf를 생성합니다

# .env 파일 로드
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# 환경변수가 설정되지 않은 경우 기본값 사용
DEVELOPER_IP_1=${DEVELOPER_IP_1:-"127.0.0.1"}
DEVELOPER_IP_2=${DEVELOPER_IP_2:-"127.0.0.1"}
MY_IP=${MY_IP:-"127.0.0.1"}

# nginx.conf.template에서 nginx.conf 생성
envsubst '${DEVELOPER_IP_1} ${DEVELOPER_IP_2} ${MY_IP}' < nginx.conf.template > nginx.conf

echo "nginx.conf 파일이 생성되었습니다."
echo "허용된 IP 목록:"
echo "- 개발자 1: ${DEVELOPER_IP_1}"
echo "- 개발자 2: ${DEVELOPER_IP_2}"
echo "- 본인: ${MY_IP}"