# 배포 가이드

## 포트 설정

- **443 포트 (HTTPS)**: 모든 IP 접근 허용
- **8080 포트 (HTTP)**: 특정 개발자 IP만 접근 허용

## 배포 순서

1. **환경변수 설정**
   ```bash
   # .env 파일에서 개발자 IP 주소 수정
   DEVELOPER_IP_1=실제_개발자1_IP
   DEVELOPER_IP_2=실제_개발자2_IP  
   MY_IP=본인_실제_IP
   ```

2. **nginx 설정 생성**
   ```bash
   ./generate-nginx-conf.sh
   ```

3. **SSL 인증서 생성** (처음 한 번만)
   ```bash
   ./generate-ssl.sh
   ```

4. **서비스 시작**
   ```bash
   docker-compose up -d
   ```

## IP 주소 확인

```bash
./update-nginx-ips.sh
```

## 보안 주의사항

- `.env` 파일은 git에 커밋되지 않습니다
- `nginx.conf` 파일은 git에 커밋되지 않습니다 (IP 정보 포함)
- `nginx.conf.template`만 git에 포함됩니다
- SSL 인증서 파일들은 git에 커밋되지 않습니다

## 파일 구조

```
├── nginx.conf.template     # nginx 설정 템플릿 (git 포함)
├── nginx.conf             # 실제 nginx 설정 (git 제외)
├── .env                   # 환경변수 (git 제외)
├── ssl/                   # SSL 인증서 (git 제외)
├── generate-nginx-conf.sh # nginx 설정 생성 스크립트
├── generate-ssl.sh        # SSL 인증서 생성 스크립트
└── update-nginx-ips.sh    # IP 확인 도구
```