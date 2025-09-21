#!/bin/bash

# 개발자 IP 업데이트 스크립트
# nginx.conf 파일의 IP 주소를 업데이트합니다

echo "현재 IP 주소를 확인합니다..."
echo "외부 IP: $(curl -s ifconfig.me)"
echo "로컬 IP: $(ipconfig getifaddr en0 2>/dev/null || ip route get 1 | head -1 | cut -d' ' -f7)"

echo ""
echo "nginx.conf 파일을 수정하여 다음 IP 주소들을 허용 목록에 추가하세요:"
echo "1. 본인 IP: $(curl -s ifconfig.me)"
echo "2. 프론트엔드 개발자 1 IP: [수동 입력 필요]"
echo "3. 프론트엔드 개발자 2 IP: [수동 입력 필요]"

echo ""
echo "nginx.conf 파일에서 다음 부분을 수정하세요:"
echo "allow [개발자1_실제_IP];  # 개발자 1 IP"
echo "allow [개발자2_실제_IP];  # 개발자 2 IP"  
echo "allow [본인_실제_IP];     # 본인 IP"