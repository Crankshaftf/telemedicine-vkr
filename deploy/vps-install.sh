# Deploy MedConnect on Linux VPS (Ubuntu)
# Usage on server:
#   git clone ... && cd telemedicine-vkr
#   chmod +x deploy/vps-install.sh && sudo ./deploy/vps-install.sh

set -e

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$APP_DIR"

echo "=== MedConnect VPS install ==="

if ! command -v docker &>/dev/null; then
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
fi

if [ ! -f .env.prod ]; then
    SECRET=$(openssl rand -hex 32)
    cat > .env.prod <<EOF
POSTGRES_USER=telemed
POSTGRES_PASSWORD=$(openssl rand -hex 16)
POSTGRES_DB=telemedicine
SECRET_KEY=$SECRET
API_PORT=8000
EOF
    echo "Created .env.prod with random passwords"
fi

export $(grep -v '^#' .env.prod | xargs)

docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

PUBLIC_IP=$(curl -s ifconfig.me || hostname -I | awk '{print $1}')

echo ""
echo "Deployed!"
echo "  API:    http://${PUBLIC_IP}:8000/"
echo "  Swagger http://${PUBLIC_IP}:8000/docs"
echo "  Panel:  http://${PUBLIC_IP}:8000/panel/"
echo ""
echo "Open port 8000 in cloud firewall (AWS Security Group / etc.)"
echo ""
echo "Build APK on PC:"
echo "  .\\scripts\\build-apk.ps1 -ApiUrl \"http://${PUBLIC_IP}:8000/\""
