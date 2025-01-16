// Send 버튼 클릭 이벤트 추가
const sendButton = document.getElementById("sendButton"); // Send 버튼 DOM 요소 가져오기
const messageInput = document.getElementById("messageInput"); // 메시지 입력창
const chatMessages = document.getElementById("chatMessages"); // 채팅 메시지 영역
const createGroup = document.getElementById("createGroup");

sendButton.addEventListener("click", sendMessage); // Send 버튼 클릭 시 sendMessage 호출

// WebSocket 초기화 변수
let socket = null;

// 단체 채팅방 목록 가져오기
function fetchGroupChatRooms() {
    fetch('/music-game/rooms')
        .then(response => response.json())
        .then(data => {
            const groupList = document.getElementById('groupList');
            groupList.innerHTML = ''; // 현재 리스트 초기화

            data.forEach(room => {
                const groupItem = document.createElement('div');
                groupItem.className = 'group-item';

                const groupName = document.createElement('span');
                groupName.textContent = `${room.name} (${room.participation}/${room.maxParticipation})`;

                const joinButton = document.createElement('button');
                joinButton.textContent = 'Join';
                joinButton.onclick = () => joinGroupChat(room.id, room.name);

                groupItem.appendChild(groupName);
                groupItem.appendChild(joinButton);
                groupList.appendChild(groupItem);
            });
        })
        .catch(error => console.error('Error fetching group chat rooms:', error));
}

function joinGroupChat(roomId, roomName) {
    if (!roomId) {
        alert("Invalid room ID!");
        return;
    }

    console.log(`Joining room with ID: ${roomId}`);
    fetch('/music-game/join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ roomId }),
    })
        .then(async response => {
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.errorMessage || "Failed to join room");
            }
            return response.json();
        })
        .then(data => {
            console.log("Join success:", data);

            sessionStorage.setItem("roomId", roomId);
            sessionStorage.setItem("roomName", roomName);

            // UI 업데이트
            const groupListContainer = document.getElementById('groupListContainer');
            const chatBox = document.getElementById('chatContainer');
            const groupTitle = document.getElementById('groupTitle');
            groupTitle.textContent = roomName;

            groupListContainer.style.display = 'none';
            chatBox.style.display = 'flex';

            // 리더보드 업데이트
            fetchParticipants(roomId);

            // WebSocket 연결 설정
            connectToRoom(roomId, roomName);
        })
        .catch(error => {
            console.error("Error joining room:", error);
            alert(error.message);
        });
}


let reconnectAttempts = 0;

// WebSocket 연결 및 메시지 처리
function connectToRoom(roomId, roomName) {
    if (socket) socket.close(); // 이전 연결 종료

    socket = new WebSocket(`ws://localhost:8080/ws/music-game/${roomId}`);

    socket.onopen = () => {
        console.log(`Connected to room: ${roomName}`);
    };

    socket.onmessage = (event) => handleWebSocketMessage(event);

    socket.onclose = () => {
        console.log(`WebSocket closed for room: ${roomName}`);
        // 재접속 로직 없음
    };

    socket.onerror = (error) => {
        console.error("WebSocket error:", error);
    };
}

function handleWebSocketMessage(event) {
    try {
        if (typeof event.data === "string") {
            // JSON 문자열 처리
            const parsedData = JSON.parse(event.data);

            // 메시지 타입에 따른 분기 처리
            switch (parsedData.type) {
                case "text":
                    displayMessage(parsedData.content, "received", parsedData.userName || "Unknown");
                    break;
                case "user-joined":
                    displayMessage(parsedData.content, "system"); // 사용자 입장 메시지
                    break;

                case "user-left":
                    displayMessage(parsedData.content, "system"); // 사용자 퇴장 메시지
                    break;

                default:
                    console.warn("Unknown message type:", parsedData.type);
            }
        }
    } catch (error) {
        console.error("Error handling WebSocket message:", error);
        console.log("Raw message data:", event.data);
    }
}

// 메시지 전송 함수
function sendMessage() {
    const message = messageInput.value.trim();

    if (!message) {
        console.error("Message is empty. Please type a message before sending.");
        return;
    }

    if (!socket || socket.readyState !== WebSocket.OPEN) {
        console.error("WebSocket is not open. Unable to send the message.");
        return;
    }

    // 메시지 객체 생성
    const messageData = {
        type: "text",
        content: message,
        timestamp: new Date().toISOString(), // 전송 시간
    };

    try {
        socket.send(JSON.stringify(messageData)); // WebSocket으로 메시지 전송
        displayMessage(message, "sent");
        messageInput.value = ""; // 입력창 초기화
    } catch (error) {
        console.error("Error sending message:", error);
    }
}
async function createGroupChat(groupName) {
    const response = await fetch('/music-game/rooms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: groupName }),
    });

    if (!response.ok) {
        throw new Error(`Server error: ${response.status}`);
    }

    const data = await response.json();
    console.log("Group created successfully:", data);
    return data; // 생성된 방 정보 반환
}

document.getElementById("createGroup").addEventListener("click", async () => {
    const groupNameInput = document.getElementById("groupName");
    const groupName = groupNameInput.value.trim();

    if (!groupName) {
        alert("Group name cannot be empty!");
        return;
    }
    try {
        const groupData = await createGroupChat(groupName);
        alert(`Group "${groupData.name}" created successfully!`);

        joinGroupChat(groupData.id, groupData.name); // 생성 후 바로 방에 참여
        fetchParticipants(groupData.id); // 리더보드 업데이트

        groupNameInput.value = ""; // 입력창 초기화
    } catch (error) {
        console.error("Error creating group:", error);
        alert("Failed to create group. Please try again.");
    }
});


// 메시지 표시 함수
function displayMessage(message, messageType, userName = "You") {
    const messageDiv = document.createElement("div");
    messageDiv.className = messageType === "sent" ? "message-sent-wrapper" : "message-received-wrapper";

    const timeString = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

    messageDiv.innerHTML = `
        <div class="message-container">
            <div class="message-header">
                <span class="message-username">${userName}</span>
                <span class="message-time">${timeString}</span>
            </div>
            <div class="message-content">${message}</div>
        </div>
    `;

    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}
let shouldReconnect = true; // WebSocket 재접속 여부를 제어하는 플래그

// 방 나가기 API 호출 및 WebSocket 종료
function leaveRoom() {
    fetch('/music-game/leave', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Failed to leave the group chat");
            }
            console.log("Successfully left the group chat");

        })
        .catch(error => {
            console.error("Error leaving group chat:", error);
        });
    // WebSocket 연결 종료
    if (socket) {
        socket.close(); // WebSocket 종료
        socket = null;  // WebSocket 참조 해제
    }
}

function fetchParticipants(roomId) {
    fetch(`/music-game/participants?roomId=${roomId}`)
        .then(response => response.json())
        .then(data => {
            const leaderboardContainer = document.getElementById('leaderboardContainer');
            const leaderboard = document.getElementById('leaderboard');

            leaderboard.innerHTML = ''; // 기존 리스트 초기화

            data.participants.forEach(participant => {
                const participantItem = document.createElement('li');
                participantItem.textContent = participant; // 이름 표시
                leaderboard.appendChild(participantItem);
            });

            leaderboardContainer.style.display = 'block'; // 리더보드 표시
        })
        .catch(error => console.error('Error fetching participants:', error));
}

function recoverRoomData() {
    const roomId = sessionStorage.getItem("roomId");
    const roomName = sessionStorage.getItem("roomName");

    if (roomId && roomName) {
        console.log(`Recovering room: ${roomName} (${roomId})`);
        joinGroupChat(roomId, roomName); // 이전 방에 재연결
    }
}

let isReloading = false;
window.addEventListener("beforeunload", (event) => {

    if (isReloading) {
        console.log("Page is reloading");
    } else {
        console.log("Page is closing");
        leaveRoom();
        sessionStorage.removeItem("roomId");
        sessionStorage.removeItem("roomName");
    }
});

// 새로고침 여부 감지
window.addEventListener("keydown", (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === "r") {
        isReloading = true; // Ctrl+R 또는 Cmd+R로 새로고침 감지
    }
});

window.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "hidden") {
        if (!isReloading) {
            leaveRoom(); // 페이지가 닫힐 때 leaveRoom 호출
        }
    }
});

window.onload = () => {
    console.log("window onLoad")
    const roomId = sessionStorage.getItem("roomId");
    const roomName = sessionStorage.getItem("roomName");
    if (roomId && roomName) {
        console.log("recovery")

        recoverRoomData(); // 새로고침 시 방 복구
    } else {
        console.log("main")

        fetchGroupChatRooms(); // 새로고침 시 방 정보가 없으면 기본 방 목록 불러오기
    }
};

document.getElementById("backButton").addEventListener("click", () => {
    leaveRoom(); // 방 나가기 API 호출
    sessionStorage.removeItem("roomId");
    sessionStorage.removeItem("roomName");
    resetUI();   // UI 초기화
    fetchGroupChatRooms(); // 새로고침 시 방 정보가 없으면 기본 방 목록 불러오기
});

// resetUI 함수 수정 (WebSocket 종료는 leaveRoom에서 처리)
function resetUI() {
    document.getElementById("groupListContainer").style.display = "block";
    document.getElementById("chatContainer").style.display = "none";
    chatMessages.innerHTML = "";
}
