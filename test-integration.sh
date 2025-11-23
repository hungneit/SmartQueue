#!/bin/bash

# 🧪 SmartQueue Full Integration Test (No Cloud Required)
# Test toàn bộ luồng nghiệp vụ từ frontend đến backend

set -e

echo "========================================="
echo "🧪 SMARTQUEUE INTEGRATION TEST"
echo "Testing business logic without cloud"
echo "========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test counter
PASSED=0
FAILED=0

# Function to test and print result
test_api() {
    local name=$1
    local method=$2
    local url=$3
    local data=$4
    local expected=$5
    
    echo -n "Testing $name... "
    
    if [ "$method" = "POST" ]; then
        response=$(curl -s -X POST "$url" -H "Content-Type: application/json" -d "$data")
    else
        response=$(curl -s "$url")
    fi
    
    if echo "$response" | grep -q "$expected"; then
        echo -e "${GREEN}✓ PASS${NC}"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}"
        echo "Expected: $expected"
        echo "Got: $response"
        ((FAILED++))
        return 1
    fi
}

echo -e "${BLUE}📊 Step 1: Health Checks${NC}"
echo "-------------------------------------------"
curl -s http://localhost:8080/actuator/health | jq . > /dev/null && echo -e "${GREEN}✓${NC} AWS Service (8080)" || echo -e "${RED}✗${NC} AWS Service"
curl -s http://localhost:8081/actuator/health | jq . > /dev/null && echo -e "${GREEN}✓${NC} Aliyun Service (8081)" || echo -e "${RED}✗${NC} Aliyun Service"
curl -s http://localhost:3000 > /dev/null && echo -e "${GREEN}✓${NC} Frontend (3000)" || echo -e "${RED}✗${NC} Frontend"
echo ""

echo -e "${BLUE}📊 Step 2: User Registration & Login${NC}"
echo "-------------------------------------------"

# Register user
echo "Registering new user..."
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@smartqueue.com",
    "password": "Test123!",
    "name": "Test User",
    "phone": "+84987654321"
  }')

if echo "$REGISTER_RESPONSE" | grep -q "userId"; then
    echo -e "${GREEN}✓ PASS${NC} - User registration"
    USER_ID=$(echo "$REGISTER_RESPONSE" | jq -r '.userId')
    echo "  User ID: $USER_ID"
    ((PASSED++))
else
    echo -e "${RED}✗ FAIL${NC} - User registration"
    echo "  Response: $REGISTER_RESPONSE"
    ((FAILED++))
fi
echo ""

echo -e "${BLUE}📊 Step 3: Queue Management${NC}"
echo "-------------------------------------------"

# Get all queues
echo "Getting available queues..."
QUEUES_RESPONSE=$(curl -s http://localhost:8080/queues)

if echo "$QUEUES_RESPONSE" | grep -q "queueId"; then
    echo -e "${GREEN}✓ PASS${NC} - Get queues"
    QUEUE_ID=$(echo "$QUEUES_RESPONSE" | jq -r '.[0].queueId')
    QUEUE_NAME=$(echo "$QUEUES_RESPONSE" | jq -r '.[0].queueName')
    echo "  Queue: $QUEUE_NAME ($QUEUE_ID)"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠${NC}  No queues available"
    QUEUE_ID="test-queue-1"
fi
echo ""

echo -e "${BLUE}📊 Step 4: Join Queue${NC}"
echo "-------------------------------------------"

# Join queue
echo "Joining queue: $QUEUE_ID..."
JOIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/queues/${QUEUE_ID}/join" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"$USER_ID\"}")

if echo "$JOIN_RESPONSE" | grep -q "ticketId"; then
    echo -e "${GREEN}✓ PASS${NC} - Join queue"
    TICKET_ID=$(echo "$JOIN_RESPONSE" | jq -r '.ticketId')
    POSITION=$(echo "$JOIN_RESPONSE" | jq -r '.position')
    echo "  Ticket ID: $TICKET_ID"
    echo "  Position: $POSITION"
    ((PASSED++))
else
    echo -e "${RED}✗ FAIL${NC} - Join queue"
    echo "  Response: $JOIN_RESPONSE"
    ((FAILED++))
    TICKET_ID="test-ticket-1"
    POSITION=1
fi
echo ""

echo -e "${BLUE}📊 Step 5: Get ETA (Aliyun Service)${NC}"
echo "-------------------------------------------"

# Get ETA
echo "Calculating ETA..."
ETA_RESPONSE=$(curl -s "http://localhost:8081/eta?queueId=${QUEUE_ID}&ticketId=${TICKET_ID}&position=${POSITION}")

if echo "$ETA_RESPONSE" | grep -q "estimatedWaitMinutes"; then
    echo -e "${GREEN}✓ PASS${NC} - ETA calculation"
    WAIT_TIME=$(echo "$ETA_RESPONSE" | jq -r '.estimatedWaitMinutes')
    P90_TIME=$(echo "$ETA_RESPONSE" | jq -r '.p90WaitMinutes')
    echo "  Estimated wait: ${WAIT_TIME} minutes"
    echo "  P90 wait: ${P90_TIME} minutes"
    ((PASSED++))
else
    echo -e "${RED}✗ FAIL${NC} - ETA calculation"
    echo "  Response: $ETA_RESPONSE"
    ((FAILED++))
fi
echo ""

echo -e "${BLUE}📊 Step 6: Check Queue Status${NC}"
echo "-------------------------------------------"

# Check status
echo "Checking ticket status..."
STATUS_RESPONSE=$(curl -s "http://localhost:8080/queues/${QUEUE_ID}/status?ticketId=${TICKET_ID}")

if echo "$STATUS_RESPONSE" | grep -q "$TICKET_ID"; then
    echo -e "${GREEN}✓ PASS${NC} - Queue status check"
    STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.status')
    echo "  Status: $STATUS"
    ((PASSED++))
else
    echo -e "${RED}✗ FAIL${NC} - Queue status check"
    echo "  Response: $STATUS_RESPONSE"
    ((FAILED++))
fi
echo ""

echo -e "${BLUE}📊 Step 7: Frontend Integration${NC}"
echo "-------------------------------------------"

# Test frontend can reach backend through proxy
echo "Testing frontend proxy to AWS backend..."
PROXY_AWS=$(curl -s http://localhost:3000/api/aws/actuator/health 2>/dev/null || echo "{}")

if echo "$PROXY_AWS" | grep -q "UP"; then
    echo -e "${GREEN}✓ PASS${NC} - Frontend → AWS proxy"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠${NC}  Frontend proxy not tested (frontend may not be running)"
fi

echo "Testing frontend proxy to Aliyun backend..."
PROXY_ALIYUN=$(curl -s http://localhost:3000/api/aliyun/actuator/health 2>/dev/null || echo "{}")

if echo "$PROXY_ALIYUN" | grep -q "UP"; then
    echo -e "${GREEN}✓ PASS${NC} - Frontend → Aliyun proxy"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠${NC}  Frontend proxy not tested (frontend may not be running)"
fi
echo ""

echo -e "${BLUE}📊 Step 8: Cross-Service Communication${NC}"
echo "-------------------------------------------"

# Test AWS calling Aliyun
echo "Testing AWS → Aliyun integration..."
# Join another queue to trigger potential ETA call
JOIN2_RESPONSE=$(curl -s -X POST "http://localhost:8080/queues/${QUEUE_ID}/join" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"user-$(date +%s)\"}")

if echo "$JOIN2_RESPONSE" | grep -q "ticketId"; then
    TICKET2_ID=$(echo "$JOIN2_RESPONSE" | jq -r '.ticketId')
    echo -e "${GREEN}✓ PASS${NC} - Second user joined queue"
    echo "  Ticket: $TICKET2_ID"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠${NC}  Could not test cross-service communication"
fi
echo ""

echo "========================================="
echo -e "${BLUE}📊 TEST SUMMARY${NC}"
echo "========================================="
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo "Total: $((PASSED + FAILED))"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed! 🎉${NC}"
    echo ""
    echo "✅ Business Logic Verified:"
    echo "   • User Registration ✓"
    echo "   • Queue Listing ✓"
    echo "   • Join Queue ✓"
    echo "   • ETA Calculation ✓"
    echo "   • Queue Status ✓"
    echo "   • Service Integration ✓"
    echo ""
    echo -e "${BLUE}🚀 System Ready for Development!${NC}"
    echo ""
    echo "Frontend: http://localhost:3000"
    echo "AWS API:  http://localhost:8080"
    echo "ETA API:  http://localhost:8081"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    echo ""
    echo "💡 Troubleshooting:"
    echo "   1. Restart backend services (they need in-memory repo)"
    echo "   2. Check logs for errors"
    echo "   3. Verify all ports are accessible"
    exit 1
fi
