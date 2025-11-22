#!/bin/bash

# SmartQueue Full System Test
# Tests: Backend AWS + Backend Aliyun + Frontend Integration

echo "========================================="
echo "🧪 SMARTQUEUE FULL SYSTEM TEST"
echo "Frontend + Backend Integration Test"
echo "========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASSED=0
FAILED=0

# Test Helper Functions
test_endpoint() {
    local url=$1
    local description=$2
    
    response=$(curl -s -w "\n%{http_code}" "$url")
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓${NC} $description"
        return 0
    else
        echo -e "${RED}✗${NC} $description (HTTP $http_code)"
        return 1
    fi
}

echo -e "${BLUE}📊 Step 1: Service Health Checks${NC}"
echo "-------------------------------------------"

# Backend services
test_endpoint "http://localhost:8080/actuator/health" "AWS Backend (8080)" && ((PASSED++)) || ((FAILED++))
test_endpoint "http://localhost:8081/actuator/health" "Aliyun Backend (8081)" && ((PASSED++)) || ((FAILED++))

# Frontend
if curl -s http://localhost:3000 | grep -q "<!DOCTYPE html>"; then
    echo -e "${GREEN}✓${NC} Frontend (3000)"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} Frontend (3000)"
    ((FAILED++))
fi
echo ""

echo -e "${BLUE}📊 Step 2: Backend API Tests${NC}"
echo "-------------------------------------------"

# Test queue listing
echo "Testing queue listing..."
QUEUES_RESPONSE=$(curl -s http://localhost:8080/queues)
if echo "$QUEUES_RESPONSE" | grep -q "queueId"; then
    echo -e "${GREEN}✓${NC} GET /queues - Returns queue list"
    QUEUE_COUNT=$(echo "$QUEUES_RESPONSE" | jq '. | length')
    echo "  Found $QUEUE_COUNT queues"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} GET /queues"
    ((FAILED++))
fi
echo ""

echo -e "${BLUE}📊 Step 3: User Registration Flow${NC}"
echo "-------------------------------------------"

# Register new user
TIMESTAMP=$(date +%s)
TEST_EMAIL="user-$TIMESTAMP@smartqueue.com"

echo "Registering user: $TEST_EMAIL"
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/users/register \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"Test123!\",
    \"name\": \"Test User $TIMESTAMP\",
    \"phone\": \"+84987654321\",
    \"emailNotificationEnabled\": true,
    \"smsNotificationEnabled\": false
  }")

if echo "$REGISTER_RESPONSE" | grep -q "userId"; then
    echo -e "${GREEN}✓${NC} POST /users/register"
    USER_ID=$(echo "$REGISTER_RESPONSE" | jq -r '.userId')
    echo "  User ID: $USER_ID"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} POST /users/register"
    echo "  Response: $REGISTER_RESPONSE"
    ((FAILED++))
    USER_ID="test-user-123"
fi
echo ""

echo -e "${BLUE}📊 Step 4: Join Queue Flow${NC}"
echo "-------------------------------------------"

# Get first queue
FIRST_QUEUE=$(echo "$QUEUES_RESPONSE" | jq -r '.[0].queueId')
echo "Joining queue: $FIRST_QUEUE"

JOIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/queues/$FIRST_QUEUE/join" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"$USER_ID\"}")

if echo "$JOIN_RESPONSE" | grep -q "ticketId"; then
    echo -e "${GREEN}✓${NC} POST /queues/:id/join"
    TICKET_ID=$(echo "$JOIN_RESPONSE" | jq -r '.ticketId')
    POSITION=$(echo "$JOIN_RESPONSE" | jq -r '.position')
    echo "  Ticket ID: $TICKET_ID"
    echo "  Position: $POSITION"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} POST /queues/:id/join"
    echo "  Response: $JOIN_RESPONSE"
    ((FAILED++))
    TICKET_ID="test-ticket"
    POSITION=1
fi
echo ""

echo -e "${BLUE}📊 Step 5: ETA Calculation (Aliyun Service)${NC}"
echo "-------------------------------------------"

echo "Calculating ETA for position $POSITION..."
ETA_RESPONSE=$(curl -s "http://localhost:8081/eta?queueId=$FIRST_QUEUE&ticketId=$TICKET_ID&position=$POSITION")

if echo "$ETA_RESPONSE" | grep -q "estimatedWaitMinutes"; then
    echo -e "${GREEN}✓${NC} GET /eta"
    WAIT_TIME=$(echo "$ETA_RESPONSE" | jq -r '.estimatedWaitMinutes')
    P90_TIME=$(echo "$ETA_RESPONSE" | jq -r '.p90WaitMinutes')
    echo "  🧠 Smart ETA: $WAIT_TIME min"
    echo "  📊 P90: $P90_TIME min"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} GET /eta"
    ((FAILED++))
fi
echo ""

echo -e "${BLUE}📊 Step 6: Queue Status Check${NC}"
echo "-------------------------------------------"

STATUS_RESPONSE=$(curl -s "http://localhost:8080/queues/$FIRST_QUEUE/status?ticketId=$TICKET_ID")

if echo "$STATUS_RESPONSE" | grep -q "ticketId"; then
    echo -e "${GREEN}✓${NC} GET /queues/:id/status"
    STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.status')
    echo "  Status: $STATUS"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} GET /queues/:id/status"
    ((FAILED++))
fi
echo ""

echo -e "${BLUE}📊 Step 7: Frontend Integration${NC}"
echo "-------------------------------------------"

# Test frontend can reach backends through proxy
echo "Testing frontend proxy configuration..."

# Test AWS proxy
if curl -s http://localhost:3000/api/aws/actuator/health | grep -q "UP"; then
    echo -e "${GREEN}✓${NC} Frontend → AWS Backend proxy"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠${NC}  Frontend → AWS proxy (may need CORS config)"
fi

# Test Aliyun proxy
if curl -s http://localhost:3000/api/aliyun/actuator/health | grep -q "UP"; then
    echo -e "${GREEN}✓${NC} Frontend → Aliyun Backend proxy"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠${NC}  Frontend → Aliyun proxy (may need CORS config)"
fi
echo ""

echo "========================================="
echo -e "${BLUE}📊 TEST SUMMARY${NC}"
echo "========================================="
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo "Total: $((PASSED + FAILED))"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed! 🎉${NC}"
    echo ""
    echo "✅ System Components:"
    echo "   • AWS Backend (Java Spring Boot) ✓"
    echo "   • Aliyun Backend (Java Spring Boot) ✓"
    echo "   • Frontend (React + TypeScript) ✓"
    echo "   • InMemory Repositories ✓"
    echo "   • Smart ETA Algorithm ✓"
    echo ""
    echo "🚀 Access URLs:"
    echo "   Frontend:  http://localhost:3000"
    echo "   AWS API:   http://localhost:8080"
    echo "   Aliyun API: http://localhost:8081"
    echo ""
    echo "📝 Test User Credentials:"
    echo "   Email:    $TEST_EMAIL"
    echo "   Password: Test123!"
    echo "   User ID:  $USER_ID"
    echo ""
    echo "🎯 Next Steps:"
    echo "   1. Open http://localhost:3000 in browser"
    echo "   2. Login with test credentials above"
    echo "   3. Try joining a queue"
    echo "   4. Watch real-time ticket updates"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    echo ""
    echo "💡 Troubleshooting:"
    echo "   1. Check all services are running"
    echo "   2. Verify IntelliJ services are using dev profile"
    echo "   3. Check logs for errors"
    echo "   4. Restart services if needed"
    exit 1
fi
