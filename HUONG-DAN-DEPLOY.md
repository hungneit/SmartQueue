# 🚀 HƯỚNG DẪN DEPLOY 2 SERVICE LÊN CLOUD

## 📋 Tổng Quan
- **Service 1 (AWS)**: Queue Management Service - port 8080
- **Service 2 (Aliyun)**: ETA Calculation Service - port 8081
- **Java Version**: Java 18
- **Deployment Method**: JAR files với nohup (không dùng Docker)

### ⚙️ Repository Mode
- **Development (local)**: `USE_IN_MEMORY=true` → Dùng InMemoryRepository (không cần DB)
- **Production (cloud)**: `USE_IN_MEMORY=false` → Dùng DynamoDB (AWS) và TableStore (Aliyun)

**Cơ chế:** Spring Boot `@ConditionalOnProperty(name = "app.use-in-memory", havingValue = "true")` chỉ tạo InMemory beans khi flag = true. Production không set flag này → tự động dùng DB repos.

---

## 🔧 BƯỚC 1: BUILD JAR FILES

### 1.1. Build Service AWS (Queue Service)
```bash
cd /Users/lehung/Documents/Cloud/SmartQueue/service-queue-aws
mvn clean package -DskipTests
ls -lh target/*.jar
```
**Output**: `service-queue-aws-1.0.0.jar`

### 1.2. Build Service Aliyun (ETA Service)
```bash
cd /Users/lehung/Documents/Cloud/SmartQueue/service-eta-aliyun
mvn clean package -DskipTests
ls -lh target/*.jar
```
**Output**: `service-eta-aliyun-1.0.0.jar`

---

## ☁️ BƯỚC 2: SETUP AWS EC2 (Service Queue)

### 2.1. Tạo EC2 Instance
1. **Đăng nhập AWS Console** → EC2 → Launch Instance
2. **Cấu hình**:
   - Name: `smartqueue-aws-service`
   - AMI: `Amazon Linux 2023`
   - Instance type: `t2.micro` (Free tier)
   - Key pair: Tạo mới hoặc dùng existing key
   - Security Group: Mở ports **22, 8080**
   - Storage: 8GB gp3 (Free tier)
3. **Launch** và đợi status = `Running`
4. **Lấy Public IP**: Ví dụ `54.251.123.45`

### 2.2. Cài Đặt Java 18 trên EC2
```bash
# SSH vào EC2
ssh -i ~/.ssh/your-key.pem ec2-user@54.251.123.45

# Cài Java 18
sudo yum update -y
sudo yum install java-18-amazon-corretto-headless -y
java -version  # Xác nhận Java 18
```

### 2.3. Tạo Thư Mục và Upload JAR
```bash
# Trên EC2 instance
sudo mkdir -p /opt/smartqueue-aws/logs
sudo chown -R ec2-user:ec2-user /opt/smartqueue-aws

# Trên máy local (terminal mới)
cd /Users/lehung/Documents/Cloud/SmartQueue
scp -i ~/.ssh/your-key.pem \
  service-queue-aws/target/service-queue-aws-1.0.0.jar \
  ec2-user@54.251.123.45:/opt/smartqueue-aws/app.jar
```

### 2.4. Tạo File Cấu Hình
**Service AWS KHÔNG CẦN access keys** vì dùng IAM Instance Profile!

```bash
# SSH vào EC2
ssh -i ~/.ssh/your-key.pem ec2-user@54.251.123.45

# Tạo file .env
cat > /opt/smartqueue-aws/.env << 'EOF'
# Spring Configuration
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# QUAN TRỌNG: Tắt InMemory mode để dùng DynamoDB thật
USE_IN_MEMORY=false

# AWS Configuration (sẽ dùng IAM Instance Profile)
AWS_REGION=ap-southeast-1
DDB_TABLE_TICKETS=smartqueue-tickets
DDB_TABLE_QUEUES=smartqueue-queues

# Service Integration (URL của Service Aliyun)
SERVICE_B_BASEURL=http://YOUR_ALIYUN_ECS_IP:8081
SERVICE_ETA_TIMEOUT=5000

# Test Configuration
TEST_KEY=LOADTEST-SECRET-KEY

# Logging
LOG_LEVEL=INFO
EOF
```

### 2.5. Tạo IAM Role cho EC2 (QUAN TRỌNG!)
Service AWS cần quyền truy cập DynamoDB:

1. **AWS Console** → IAM → Roles → Create role
2. **Trusted entity**: AWS service → EC2
3. **Permissions**: Attach `AmazonDynamoDBFullAccess`
4. **Role name**: `EC2-DynamoDB-FullAccess`
5. **EC2 Console** → Instance → Actions → Security → Modify IAM role
6. **Chọn role** `EC2-DynamoDB-FullAccess` → Update

### 2.6. Tạo Script Quản Lý

#### start.sh
```bash
cat > /opt/smartqueue-aws/start.sh << 'EOF'
#!/bin/bash
set -e

cd /opt/smartqueue-aws

# Load environment variables
set -a
source .env
set +a

# Java options
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Start application
echo "Starting SmartQueue AWS Service..."
nohup java $JAVA_OPTS -jar app.jar > logs/app.log 2>&1 &
echo $! > app.pid

echo "Service started with PID: $(cat app.pid)"
echo "Check logs: tail -f logs/app.log"
EOF

chmod +x /opt/smartqueue-aws/start.sh
```

#### stop.sh
```bash
cat > /opt/smartqueue-aws/stop.sh << 'EOF'
#!/bin/bash
cd /opt/smartqueue-aws

if [ -f app.pid ]; then
    PID=$(cat app.pid)
    echo "Stopping service (PID: $PID)..."
    kill $PID
    
    # Wait for graceful shutdown (max 30s)
    for i in {1..30}; do
        if ! ps -p $PID > /dev/null; then
            echo "Service stopped successfully"
            rm app.pid
            exit 0
        fi
        sleep 1
    done
    
    # Force kill if still running
    echo "Force killing service..."
    kill -9 $PID
    rm app.pid
else
    echo "No PID file found"
fi
EOF

chmod +x /opt/smartqueue-aws/stop.sh
```

#### restart.sh
```bash
cat > /opt/smartqueue-aws/restart.sh << 'EOF'
#!/bin/bash
cd /opt/smartqueue-aws
./stop.sh
sleep 2
./start.sh
EOF

chmod +x /opt/smartqueue-aws/restart.sh
```

### 2.7. Khởi Động Service
```bash
cd /opt/smartqueue-aws
./start.sh

# Xem logs
tail -f logs/app.log

# Test
curl http://localhost:8080/actuator/health
```

---

## ☁️ BƯỚC 3: SETUP ALIYUN ECS (Service ETA)

### 3.1. Tạo ECS Instance
1. **Đăng nhập Aliyun Console** → ECS → Create Instance
2. **Cấu hình**:
   - Region: `Singapore (ap-southeast-1)`
   - Instance type: `ecs.t5-lc1m1.small` (1C1G)
   - Image: `Ubuntu 20.04`
   - Billing: **Pay-As-You-Go** (dùng $300 trial credit)
   - Security Group: Mở ports **22, 8081**
   - System Disk: 40GB ESSD
   - Key pair: `smartqueue-key-2`
3. **Create** và lấy Public IP, ví dụ: `47.236.XXX.XXX`

### 3.2. Cài Đặt Java 18 trên ECS
```bash
# SSH vào ECS
ssh -i ~/.ssh/smartqueue-key-2.pem root@47.236.XXX.XXX

# Cài Java 18
apt update
apt install -y openjdk-18-jdk-headless
java -version  # Xác nhận Java 18
```

### 3.3. Tạo Thư Mục và Upload JAR
```bash
# Trên ECS instance
mkdir -p /opt/smartqueue-aliyun/logs

# Trên máy local (terminal mới)
cd /Users/lehung/Documents/Cloud/SmartQueue
scp -i ~/.ssh/smartqueue-key-2.pem \
  service-eta-aliyun/target/service-eta-aliyun-1.0.0.jar \
  root@47.236.XXX.XXX:/opt/smartqueue-aliyun/app.jar
```

### 3.4. Tạo Aliyun AccessKey (QUAN TRỌNG!)
Service Aliyun **CÓ CẦN** access keys để truy cập TableStore:

1. **Aliyun Console** → AccessKey Management
2. **Create AccessKey** → Lưu lại:
   - `AccessKey ID`: LTAI5tXXXXXXXXXX
   - `AccessKey Secret`: YourSecretXXXXXXXX

### 3.5. Tạo File Cấu Hình
```bash
# SSH vào ECS
ssh -i ~/.ssh/smartqueue-key-2.pem root@47.236.XXX.XXX

# Tạo file .env (ĐIỀN THÔNG TIN THẬT)
cat > /opt/smartqueue-aliyun/.env << 'EOF'
# Spring Configuration
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8081

# Aliyun Configuration (ĐIỀN ACCESS KEY THẬT!)
ALIYUN_ACCESS_KEY_ID=LTAI5tXXXXXXXXXX
ALIYUN_ACCESS_KEY_SECRET=YourSecretXXXXXXXX
ALIYUN_REGION=ap-southeast-1

# TableStore Configuration
ALIYUN_TABLESTORE_ENDPOINT=https://smartq-ots-prod.ap-southeast-1.ots.aliyuncs.com
ALIYUN_TABLESTORE_INSTANCE=smartq-ots-prod
ALIYUN_TABLESTORE_ENABLED=true

# Test Configuration
TEST_KEY=LOADTEST-SECRET-KEY

# Logging
LOG_LEVEL=INFO
EOF

# QUAN TRỌNG: Bảo mật file chứa credentials
chmod 600 /opt/smartqueue-aliyun/.env
```

### 3.6. Tạo Script Quản Lý

#### start.sh
```bash
cat > /opt/smartqueue-aliyun/start.sh << 'EOF'
#!/bin/bash
set -e

cd /opt/smartqueue-aliyun

# Load environment variables
set -a
source .env
set +a

# Java options
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Start application
echo "Starting SmartQueue Aliyun Service..."
nohup java $JAVA_OPTS -jar app.jar > logs/app.log 2>&1 &
echo $! > app.pid

echo "Service started with PID: $(cat app.pid)"
echo "Check logs: tail -f logs/app.log"
EOF

chmod +x /opt/smartqueue-aliyun/start.sh
```

#### stop.sh
```bash
cat > /opt/smartqueue-aliyun/stop.sh << 'EOF'
#!/bin/bash
cd /opt/smartqueue-aliyun

if [ -f app.pid ]; then
    PID=$(cat app.pid)
    echo "Stopping service (PID: $PID)..."
    kill $PID
    
    # Wait for graceful shutdown (max 30s)
    for i in {1..30}; do
        if ! ps -p $PID > /dev/null; then
            echo "Service stopped successfully"
            rm app.pid
            exit 0
        fi
        sleep 1
    done
    
    # Force kill if still running
    echo "Force killing service..."
    kill -9 $PID
    rm app.pid
else
    echo "No PID file found"
fi
EOF

chmod +x /opt/smartqueue-aliyun/stop.sh
```

#### restart.sh
```bash
cat > /opt/smartqueue-aliyun/restart.sh << 'EOF'
#!/bin/bash
cd /opt/smartqueue-aliyun
./stop.sh
sleep 2
./start.sh
EOF

chmod +x /opt/smartqueue-aliyun/restart.sh
```

### 3.7. Khởi Động Service
```bash
cd /opt/smartqueue-aliyun
./start.sh

# Xem logs
tail -f logs/app.log

# Test
curl http://localhost:8081/actuator/health
```

---

## 🔗 BƯỚC 4: KẾT NỐI 2 SERVICE

### 4.1. Cập Nhật URL Service Aliyun trong AWS Service
```bash
# SSH vào AWS EC2
ssh -i ~/.ssh/your-key.pem ec2-user@54.251.123.45

# Sửa file .env
nano /opt/smartqueue-aws/.env
# Thay YOUR_ALIYUN_ECS_IP thành IP thật của Aliyun ECS
# SERVICE_B_BASEURL=http://47.236.XXX.XXX:8081

# Restart service
cd /opt/smartqueue-aws
./restart.sh
```

### 4.2. Test Kết Nối
```bash
# Từ AWS EC2, test gọi sang Aliyun ECS
curl http://47.236.XXX.XXX:8081/actuator/health

# Từ máy local, test cả 2 service
curl http://54.251.123.45:8080/actuator/health  # AWS Service
curl http://47.236.XXX.XXX:8081/actuator/health  # Aliyun Service
```

---

## 🧪 BƯỚC 5: KIỂM TRA VÀ TEST

### 5.1. Health Check
```bash
# Service AWS
curl http://54.251.123.45:8080/actuator/health

# Service Aliyun
curl http://47.236.XXX.XXX:8081/actuator/health
```

### 5.2. Xem Logs
```bash
# AWS Service
ssh -i ~/.ssh/your-key.pem ec2-user@54.251.123.45
tail -f /opt/smartqueue-aws/logs/app.log

# Aliyun Service
ssh -i ~/.ssh/smartqueue-key-2.pem root@47.236.XXX.XXX
tail -f /opt/smartqueue-aliyun/logs/app.log
```

### 5.3. Kiểm Tra Process
```bash
# AWS EC2
ps aux | grep java
cat /opt/smartqueue-aws/app.pid

# Aliyun ECS
ps aux | grep java
cat /opt/smartqueue-aliyun/app.pid
```

### 5.4. Test API Endpoint (Ví dụ)
```bash
# Tạo queue
curl -X POST http://54.251.123.45:8080/api/queues \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Queue", "description": "Testing"}'

# Lấy ETA
curl http://47.236.XXX.XXX:8081/api/eta/calculate?queueId=xxx
```

---

## 🔥 BƯỚC 6: SETUP AUTO-START (TÙY CHỌN)

### 6.1. Systemd Service cho AWS EC2
```bash
sudo tee /etc/systemd/system/smartqueue-aws.service > /dev/null << 'EOF'
[Unit]
Description=SmartQueue AWS Service
After=network.target

[Service]
Type=forking
User=ec2-user
WorkingDirectory=/opt/smartqueue-aws
ExecStart=/opt/smartqueue-aws/start.sh
ExecStop=/opt/smartqueue-aws/stop.sh
Restart=on-failure
RestartSec=10s

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable smartqueue-aws
sudo systemctl start smartqueue-aws
sudo systemctl status smartqueue-aws
```

### 6.2. Systemd Service cho Aliyun ECS
```bash
sudo tee /etc/systemd/system/smartqueue-aliyun.service > /dev/null << 'EOF'
[Unit]
Description=SmartQueue Aliyun Service
After=network.target

[Service]
Type=forking
User=root
WorkingDirectory=/opt/smartqueue-aliyun
ExecStart=/opt/smartqueue-aliyun/start.sh
ExecStop=/opt/smartqueue-aliyun/stop.sh
Restart=on-failure
RestartSec=10s

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable smartqueue-aliyun
sudo systemctl start smartqueue-aliyun
sudo systemctl status smartqueue-aliyun
```

---

## 📝 CHECKLIST TRƯỚC KHI DEPLOY

### AWS Service:
- ✅ EC2 instance đã tạo (t2.micro)
- ✅ Security Group mở port 22, 8080
- ✅ IAM Role có quyền DynamoDB (QUAN TRỌNG!)
- ✅ Java 18 đã cài
- ✅ JAR file đã build và upload
- ✅ File .env đã cấu hình
- ✅ Scripts (start/stop/restart) đã tạo

### Aliyun Service:
- ✅ ECS instance đã tạo (ecs.t5-lc1m1.small)
- ✅ Security Group mở port 22, 8081
- ✅ AccessKey đã tạo (LTAI5t...)
- ✅ Java 18 đã cài
- ✅ JAR file đã build và upload
- ✅ File .env đã cấu hình (với access keys THẬT!)
- ✅ Scripts (start/stop/restart) đã tạo
- ✅ TableStore instance đã kích hoạt

### Kết Nối:
- ✅ Service AWS có URL đúng của Service Aliyun
- ✅ Test curl từ AWS sang Aliyun thành công
- ✅ Health check cả 2 service OK

---

## 🆘 TROUBLESHOOTING

### Service không start:
```bash
# Xem logs chi tiết
tail -n 100 logs/app.log

# Kiểm tra Java version
java -version  # Phải là Java 18

# Kiểm tra port đã mở chưa
netstat -tuln | grep 8080  # hoặc 8081
```

### AWS Service lỗi DynamoDB:
```bash
# Kiểm tra IAM Role
aws sts get-caller-identity  # Chạy từ EC2

# Nếu lỗi, attach lại IAM Role cho EC2
# AWS Console → EC2 → Instance → Actions → Security → Modify IAM role
```

### Aliyun Service lỗi TableStore:
```bash
# Kiểm tra credentials
echo $ALIYUN_ACCESS_KEY_ID
echo $ALIYUN_ACCESS_KEY_SECRET

# Kiểm tra TableStore instance đã tạo chưa
# https://ots.console.aliyun.com/

# Kiểm tra endpoint đúng chưa
# Format: https://<instance-name>.<region>.ots.aliyuncs.com
```

### Không kết nối được giữa 2 service:
```bash
# Từ AWS EC2, test ping sang Aliyun
ping 47.236.XXX.XXX

# Test port
telnet 47.236.XXX.XXX 8081

# Kiểm tra Security Group Aliyun có mở port 8081 cho AWS IP không
```

---

## 💰 CHI PHÍ DỰ KIẾN

### AWS:
- EC2 t2.micro: **FREE** (750h/tháng free tier)
- DynamoDB: **FREE** (25GB storage + 200M requests)
- Total: **$0/tháng** (trong 12 tháng đầu)

### Aliyun:
- ECS ecs.t5-lc1m1.small: **$8.65/tháng**
- TableStore: **~$1-2/tháng** (tùy usage)
- Total: **~$10/tháng**
- **Dùng $300 trial credit** = **~30 tháng miễn phí**

---

## 📚 LỆNH HỮU ÍCH

```bash
# Xem log real-time
tail -f logs/app.log

# Search lỗi trong log
grep -i error logs/app.log
grep -i exception logs/app.log

# Restart service
./restart.sh

# Stop service
./stop.sh

# Start service
./start.sh

# Kiểm tra process
ps aux | grep java

# Kiểm tra port
netstat -tuln | grep 8080

# Xem resource usage
top
free -h
df -h

# Test API với curl
curl -X GET http://localhost:8080/actuator/health
curl -X POST http://localhost:8080/api/queues -H "Content-Type: application/json" -d '{...}'
```

---

## ✅ HOÀN THÀNH!

Sau khi hoàn tất tất cả các bước trên, bạn sẽ có:
- ✅ 2 service chạy trên cloud (AWS + Aliyun)
- ✅ Tự động restart khi crash
- ✅ Logs được lưu vào files
- ✅ Dễ dàng quản lý với scripts
- ✅ Chi phí tối thiểu hoặc miễn phí

**Good luck! 🚀**
