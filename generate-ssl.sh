#!/bin/bash

# SSL 인증서 생성 스크립트
# 자체 서명 인증서를 생성합니다 (개발/테스트용)

# SSL 디렉토리 생성
mkdir -p ssl/certs ssl/private

# 개인키 생성
openssl genrsa -out ssl/private/server.key 2048

# CSR(Certificate Signing Request) 생성
openssl req -new -key ssl/private/server.key -out ssl/server.csr -subj "/C=KR/ST=Seoul/L=Seoul/O=HwanYulMate/CN=localhost"

# 자체 서명 인증서 생성 (365일 유효)
openssl x509 -req -days 365 -in ssl/server.csr -signkey ssl/private/server.key -out ssl/certs/server.crt

# 임시 파일 삭제
rm ssl/server.csr

# 권한 설정
chmod 600 ssl/private/server.key
chmod 644 ssl/certs/server.crt

echo "SSL 인증서가 성공적으로 생성되었습니다."
echo "- 인증서: ssl/certs/server.crt"
echo "- 개인키: ssl/private/server.key"