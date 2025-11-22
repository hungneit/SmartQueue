#!/bin/bash

# SmartQueue AWS Service Deployment Script
# Run this on your AWS EC2 instance

set -e

echo "🚀 Starting AWS Service Deployment..."

# Check Java version
echo "📌 Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Installing Java 18..."
    sudo yum install java-18-amazon-corretto -y
fi

java -version

# Set environment variables
echo "📌 Setting environment variables..."
export AWS_REGION=${AWS_REGION:-ap-southeast-1}
export DDB_TABLE_TICKETS=${DDB_TABLE_TICKETS:-smartqueue-tickets}
export DDB_TABLE_QUEUES=${DDB_TABLE_QUEUES:-smartqueue-queues}
export SERVER_PORT=${SERVER_PORT:-8080}
export SERVICE_B_BASEURL=${SERVICE_B_BASEURL:-http://localhost:8081}
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}

# Create application directory
echo "📌 Creating application directory..."
APP_DIR="/opt/smartqueue-aws"
sudo mkdir -p $APP_DIR
cd $APP_DIR

# Download or copy JAR file
echo "📌 Deploying JAR file..."
# Assuming JAR is uploaded via SCP
if [ ! -f "service-queue-aws.jar" ]; then
    echo "❌ JAR file not found. Please upload it first."
    exit 1
fi

# Create systemd service
echo "📌 Creating systemd service..."
sudo tee /etc/systemd/system/smartqueue-aws.service > /dev/null <<EOF
[Unit]
Description=SmartQueue AWS Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=$APP_DIR
Environment="AWS_REGION=$AWS_REGION"
Environment="DDB_TABLE_TICKETS=$DDB_TABLE_TICKETS"
Environment="DDB_TABLE_QUEUES=$DDB_TABLE_QUEUES"
Environment="SERVER_PORT=$SERVER_PORT"
Environment="SERVICE_B_BASEURL=$SERVICE_B_BASEURL"
Environment="SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE"
ExecStart=/usr/bin/java -jar $APP_DIR/service-queue-aws.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Start service
echo "📌 Starting service..."
sudo systemctl daemon-reload
sudo systemctl enable smartqueue-aws
sudo systemctl start smartqueue-aws

# Check status
echo "📌 Checking service status..."
sleep 5
sudo systemctl status smartqueue-aws

echo "✅ AWS Service deployed successfully!"
echo "🔗 Health check: http://localhost:$SERVER_PORT/actuator/health"
