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
    fetch('/group-chat/rooms')
        .then(response => response.json())
        .then(data => {
            const groupList = document.getElementById('groupList');
            groupList.innerHTML = ''; // 현재 리스트 초기화

            data.forEach(room => {
                const groupItem = document.createElement('div');
                groupItem.className = 'group-item';

                const groupName = document.createElement('span');
                groupName.textContent = room.name;

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


document.addEventListener("click", async (event) => {
    if (event.target.classList.contains("translate-button")) {
        const button = event.target;
        const originalMessage = button.getAttribute("data-message");
        const translatedMessage = await translateMessage(originalMessage);
        button.textContent = translatedMessage;
    }
});

async function translateMessage(message) {
    try {
        const response = await fetch("/translate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: message }),
        });
        const data = await response.json();
        return data.translatedText;
    } catch (error) {
        console.error("Translation error:", error);
    }
}

function joinGroupChat(roomId, roomName) {
    if (!roomId) {
        alert("Invalid room ID!");
        return;
    }

    console.log(`Joining room with ID: ${roomId}`);

    // 서버에 참가 요청
    fetch('/join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ roomId }),
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to join room: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log("Join success:", data);

            // UI 업데이트
            const groupListContainer = document.getElementById('groupListContainer');
            const chatBox = document.getElementById('chatBox');
            const groupTitle = document.getElementById('groupTitle');
            groupTitle.textContent = roomName;

            groupListContainer.style.display = 'none';
            chatBox.style.display = 'flex';

            // WebSocket 연결 설정
            connectToRoom(roomId, roomName);
        })
        .catch(error => {
            console.error("Error joining room:", error);
            alert("Failed to join the room. Please try again.");
        });
}
let reconnectAttempts = 0;

function connectToRoom(roomId, roomName) {
    if (socket) socket.close();

    socket = new WebSocket(`ws://localhost:8080/ws/group/${roomId}`);

    socket.onopen = () => {
        console.log(`Connected to room: ${roomName}`);
        reconnectAttempts = 0; // 재연결 시도 초기화
    };

    socket.onmessage = (event) => handleWebSocketMessage(event);

    socket.onclose = () => {
        console.error(`Disconnected from room: ${roomName}`);
        reconnectAttempts++;
        const retryTime = Math.min(reconnectAttempts * 1000, 30000); // 최대 30초 지연
        setTimeout(() => connectToRoom(roomId, roomName), retryTime);
    };

    socket.onerror = (error) => {
        console.error("WebSocket error:", error);
    };
}

let currentFileMetadata = null;

function handleWebSocketMessage(event) {
    try {
        if (typeof event.data === "string") {
            const parsedData = JSON.parse(event.data);

            if (parsedData.type === "file") {
                currentFileMetadata = parsedData; // 메타데이터 저장
            } else if (parsedData.type === "text") {
                displayMessage(parsedData.content, "received", parsedData.userName);
            }
        } else if (event.data instanceof Blob) {
            if (currentFileMetadata) {
                const fileUrl = URL.createObjectURL(event.data);
                displayFileMessage(fileUrl, "received", currentFileMetadata.fileType);
                currentFileMetadata = null; // 메타데이터 초기화
            } else {
                console.error("Metadata not found for received file.");
            }
        }
    } catch (error) {
        console.error("Error handling WebSocket message:", error);
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
    const response = await fetch('/group-chat/rooms', {
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
            ${
        messageType === "received"
            ? `<button class="translate-button" data-message="${message}">Translate</button>`
            : ""
    }
        </div>
    `;

    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}


document.getElementById("backButton").addEventListener("click", resetUI);
document.getElementById("fileInput").addEventListener("change", async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const metadata = {
        type: "file",
        fileName: file.name,
        fileType: file.type,
    };

    try {
        // 메타데이터 전송
        socket.send(JSON.stringify(metadata));

        // 파일 데이터 전송
        const arrayBuffer = await file.arrayBuffer();
        socket.send(arrayBuffer);

        // 송신자 UI에 파일 표시
        displayFileMessage(URL.createObjectURL(file), "sent", file.type);

        // 파일 선택 초기화
        event.target.value = "";
    } catch (error) {
        console.error("Error sending file:", error);
    }
});
function displayFileMessage(fileUrl, messageType, fileType = "") {
    const messageDiv = document.createElement("div");
    messageDiv.className = messageType === "sent" ? "message-sent" : "message-received";

    if (fileType.startsWith("image")) {
        messageDiv.innerHTML = `<img src="${fileUrl}" alt="Image" style="max-width: 100%; border-radius: 10px;">`;
    } else if (fileType.startsWith("video")) {
        messageDiv.innerHTML = `<video src="${fileUrl}" controls style="max-width: 100%; border-radius: 10px;"></video>`;
    } else {
        messageDiv.innerHTML = `<a href="${fileUrl}" download style="color: #4e60d9; text-decoration: underline;">Download File</a>`;
    }

    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function resetUI() {
    document.getElementById("groupListContainer").style.display = "block";
    document.getElementById("chatBox").style.display = "none";
    chatMessages.innerHTML = "";
    if (socket) {
        socket.close(); // WebSocket 종료
        socket = null;
    }
}

// 페이지 로드 시 그룹 목록 가져오기
window.onload = fetchGroupChatRooms;