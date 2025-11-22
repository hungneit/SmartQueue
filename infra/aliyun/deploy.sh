#!/bin/bash

# SmartQueue Aliyun Service Deployment Script
# Run this on your Aliyun ECS instance

set -e

echo "🚀 Starting Aliyun Service Deployment..."

# Check Java version
echo "📌 Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Installing Java 18..."
    yum install java-18-openjdk -y
fi

java -version

# Set environment variables
echo "📌 Setting environment variables..."
export ALIYUN_AK=${ALIYUN_AK}
export ALIYUN_SK=${ALIYUN_SK}
export ALI_REGION=${ALI_REGION:-ap-southeast-1}
export OTS_ENDPOINT=${OTS_ENDPOINT}
export OTS_INSTANCE=${OTS_INSTANCE:-smartqueue-ots}
export TABLESTORE_ENABLED=${TABLESTORE_ENABLED:-true}
export DIRECTMAIL_ENABLED=${DIRECTMAIL_ENABLED:-true}
export SERVER_PORT=${SERVER_PORT:-8081}
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}

# Validate required variables
if [ -z "$ALIYUN_AK" ] || [ -z "$ALIYUN_SK" ]; then
    echo "❌ ALIYUN_AK and ALIYUN_SK must be set!"
    exit 1
fi

# Create application directory
echo "📌 Creating application directory..."
APP_DIR="/opt/smartqueue-aliyun"
mkdir -p $APP_DIR
cd $APP_DIR

# Download or copy JAR file
echo "📌 Deploying JAR file..."
if [ ! -f "service-eta-aliyun.jar" ]; then
    echo "❌ JAR file not found. Please upload it first."
    exit 1
fi

# Create systemd service
echo "📌 Creating systemd service..."
tee /etc/systemd/system/smartqueue-aliyun.service > /dev/null <<EOF
[Unit]
Description=SmartQueue Aliyun Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$APP_DIR
Environment="ALIYUN_AK=$ALIYUN_AK"
Environment="ALIYUN_SK=$ALIYUN_SK"
Environment="ALI_REGION=$ALI_REGION"
Environment="OTS_ENDPOINT=$OTS_ENDPOINT"
Environment="OTS_INSTANCE=$OTS_INSTANCE"
Environment="TABLESTORE_ENABLED=$TABLESTORE_ENABLED"
Environment="DIRECTMAIL_ENABLED=$DIRECTMAIL_ENABLED"
Environment="SERVER_PORT=$SERVER_PORT"
Environment="SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE"
ExecStart=/usr/bin/java -jar $APP_DIR/service-eta-aliyun.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Start service
echo "📌 Starting service..."
systemctl daemon-reload
systemctl enable smartqueue-aliyun
systemctl start smartqueue-aliyun

# Check status
echo "📌 Checking service status..."
sleep 5
systemctl status smartqueue-aliyun

echo "✅ Aliyun Service deployed successfully!"
echo "🔗 Health check: http://localhost:$SERVER_PORT/actuator/health"
