const statusDiv = document.getElementById("status");
const chatBox = document.getElementById("chatBox");
const chatMessages = document.getElementById("chatMessages");
const messageInput = document.getElementById("messageInput");
const sendButton = document.getElementById("sendButton");
const connectButton = document.getElementById("connectButton");

const backButton = document.getElementById("backButton");

const fileInput = document.getElementById("fileInput");
const fileUploadButton = document.getElementById("fileUploadButton");

const backToMainButton = document.getElementById("backToMainButton");

backToMainButton.addEventListener("click", () => {
    window.location.href = "/";
});

function compressImage(file, maxWidth, maxHeight) {
    return new Promise((resolve) => {
        const img = document.createElement("img");
        const canvas = document.createElement("canvas");
        const ctx = canvas.getContext("2d");

        const reader = new FileReader();
        reader.onload = (e) => {
            img.src = e.target.result;
        };
        img.onload = () => {
            const scale = Math.min(maxWidth / img.width, maxHeight / img.height);
            canvas.width = img.width * scale;
            canvas.height = img.height * scale;

            ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
            canvas.toBlob((blob) => resolve(blob), file.type, 0.7); // 70% 품질로 압축
        };
        reader.readAsDataURL(file);
    });
}
fileInput.addEventListener("change", async (event) => {
    let file = event.target.files[0]; // 변경: const → let
    if (!file) return;

    if (file.size > 1024 * 1024 * 5) { // 5MB 제한
        const compressedFile = await compressImage(file, 1024, 1024); // 1024px로 리사이즈
        if (compressedFile.size > 1024 * 1024 * 5) {
            alert("파일 크기가 너무 큽니다. 압축 후에도 5MB를 초과합니다.");
            return;
        }
        file = new File([compressedFile], file.name, { type: file.type });
    }

    const metadata = {
        type: "file",
        fileName: file.name,
        fileType: file.type,
        size: file.size,
    };

    try {
        // JSON 메타데이터 전송
        socket.send(JSON.stringify(metadata));

        // 파일 데이터 전송
        const arrayBuffer = await file.arrayBuffer();
        socket.send(arrayBuffer);

        // 내 화면에 파일 미리보기
        const fileUrl = URL.createObjectURL(file);
        displayFileMessage(fileUrl, file.type, "sent");
    } catch (error) {
        console.error("Error sending file:", error);
    }
});

function displayFileMessage(fileUrl, fileType, messageType) {
    const messageDiv = document.createElement("div");
    messageDiv.className = messageType === "sent" ? "message-sent" : "message-received";

    if (fileType.startsWith("image")) {
        messageDiv.innerHTML = `<img src="${fileUrl}" alt="Image" style="max-width: 100%;">`;
    } else if (fileType.startsWith("video")) {
        messageDiv.innerHTML = `<video src="${fileUrl}" controls style="max-width: 100%;"></video>`;
    } else {
        messageDiv.innerHTML = `<a href="${fileUrl}" download="${currentFileMetadata?.fileName || 'file'}">Download File</a>`;
    }

    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

backButton.addEventListener("click", () => {
    resetUI();
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close(); // Close the WebSocket connection on exit
    }
});




let socket = null;
function displayMessage(message, messageType) {
    const messageDiv = document.createElement("div");
    messageDiv.className = messageType === "sent" ? "message-sent" : "message-received";

    const timeString = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    if (messageType === "received") {
        // 수신 메시지에는 번역 버튼 추가
        messageDiv.innerHTML = `
            <span>${message}</span>
            <button class="translate-button" data-message="${message}">번역</button>
            <span class="message-time">${timeString}</span>`;
    } else {
        // 전송 메시지에는 번역 버튼 없음
        messageDiv.innerHTML = `
            <span>${message}</span>
            <span class="message-time">${timeString}</span>`;
    }

    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// Initialize WebSocket
function initWebSocket() {
    socket = new WebSocket("ws://localhost:8080/ws/connect");
    // socket = new WebSocket("ws://192.168.129.100:8080/ws/connect");

    socket.onopen = () => {
        statusDiv.textContent = "새로운 친구를 찾는 중...";
    };

    socket.onmessage = async (event) => {
        try {
            if (typeof event.data === "string") {
                // JSON 형식인지 확인
                if (event.data.startsWith("{") && event.data.endsWith("}")) {
                    const parsedData = JSON.parse(event.data);

                    if (parsedData.type === "file") {
                        console.log(`Receiving file metadata: ${parsedData.fileName}, ${parsedData.fileType}`);
                        currentFileMetadata = parsedData; // 메타데이터 저장
                    } else if (parsedData.type === "text") {
                        displayMessage(parsedData.content, "received"); // 수신 메시지 표시
                    }
                } else {
                    // 단순 텍스트 메시지 처리
                    console.log("Received plain text:", event.data);

                    if (event.data.startsWith("MATCH_SUCCESS:")) {
                        const opponentUser = event.data.split(":")[1];
                        statusDiv.style.display = "none";
                        connectButton.style.display = "none";
                        chatBox.style.display = "flex";
                        console.log(`Matched with: ${opponentUser}`);
                    } else if (event.data === "WAITING_FOR_MATCH") {
                        statusDiv.textContent = "Waiting for a match...";
                    } else {
                        displayMessage(event.data, "received"); // 수신 메시지 표시
                    }
                }
            } else if (event.data instanceof Blob) {
                // 바이너리 데이터 처리
                if (currentFileMetadata) {
                    const fileType = currentFileMetadata.fileType;
                    const fileUrl = URL.createObjectURL(event.data);

                    displayFileMessage(fileUrl, fileType, "received");

                    currentFileMetadata = null; // 메타데이터 초기화
                } else {
                    console.error("Metadata not found for received file.");
                }
            }
        } catch (error) {
            console.error("Error processing incoming message:", error);
        }
    };

    let reconnectAttempts = 0;
    const maxReconnectAttempts = 5;

    socket.onclose = (event) => {
        console.log(`WebSocket closed. Code: ${event.code}, Reason: ${event.reason}`);
        resetUI();
    };

    socket.onerror = (error) => {
        console.error("WebSocket error: ", error);
        resetUI();
    };
}
function isOlderThanFiveMinutes(messageTime) {
    const now = new Date();
    const messageDate = new Date(messageTime);
    return now - messageDate > 5* 60 * 1000; // 5분 이상 지났는지 확인
}

function parseTimeToDate(timeString) {
    const [period, time] = timeString.split(" ");
    const [hours, minutes] = time.split(":").map(Number);

    let fullHours = hours;
    if (period === "오후" && hours !== 12) {
        fullHours += 12;
    } else if (period === "오전" && hours === 12) {
        fullHours = 0;
    }

    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), now.getDate(), fullHours, minutes);
}
function resetUI() {
    statusDiv.textContent = "매칭하기";
    statusDiv.style.display = "block";
    connectButton.style.display = "inline-block";
    connectButton.disabled = false;
    chatBox.style.display = "none";
    chatMessages.innerHTML = ""; // Clear chat messages
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close(); // Ensure the WebSocket is closed
    }
}


connectButton.addEventListener("click", () => {
    connectButton.disabled = true;
    initWebSocket();
    statusDiv.textContent = "새로운 친구를 찾는 중...";
});

sendButton.addEventListener("click", sendMessage);

let isComposing = false;

messageInput.addEventListener("compositionstart", () => {
    isComposing = true;
});

messageInput.addEventListener("compositionend", () => {
    isComposing = false;
});

messageInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !isComposing) {
        event.preventDefault();
        sendMessage();
    }
});


chatMessages.addEventListener("scroll", () => {
    const messages = chatMessages.querySelectorAll(".message-received, .message-sent");
    let olderMessageExists = false;

    messages.forEach(message => {
        const timeElement = message.querySelector(".message-time");
        if (timeElement) {
            const timeText = timeElement.textContent; // 예: "오후 06:20"
            const messageTime = parseTimeToDate(timeText);

            if (isOlderThanFiveMinutes(messageTime)) {
                message.style.display = "none"; // 5분 이상 지난 메시지 숨기기
                olderMessageExists = true;
            } else {
                message.style.display = "flex"; // 최신 메시지는 보이기
            }
        }
    });

    // "5분 전 메시지는 볼 수 없습니다" 표시
    const olderMessageNotice = document.getElementById("olderMessageNotice");
    olderMessageNotice.style.display = olderMessageExists ? "block" : "none";
});

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

    // 메시지 메타데이터 추가
    const messageData = {
        type: "text",
        content: message,
        timestamp: new Date().toISOString(), // 메시지 전송 시간
    };

    try {
        // WebSocket으로 메시지 전송
        socket.send(JSON.stringify(messageData));

        // UI에 메시지 추가
        const messageDiv = document.createElement("div");
        messageDiv.className = "message-sent";

        const timeString = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        messageDiv.innerHTML = `<span>You: ${message}</span> <span class="message-time">${timeString}</span>`;
        chatMessages.appendChild(messageDiv);
        chatMessages.scrollTop = chatMessages.scrollHeight;

        // 입력창 초기화
        messageInput.value = "";
    } catch (error) {
        console.error("Error sending message:", error);
    }
}


document.addEventListener("click", async (event) => {
    if (event.target.classList.contains("translate-button")) {
        const button = event.target;
        const originalMessage = button.getAttribute("data-message");

        // 번역 요청
        const translatedMessage = await translateMessage(originalMessage);

        if (translatedMessage) {
            // URL 디코딩하여 사용자에게 표시
            const decodedMessage = decodeURIComponent(translatedMessage);

            // 원래 메시지 대체
            const messageElement = button.previousElementSibling;
            messageElement.textContent = decodedMessage;

            // 버튼 제거 또는 "번역 완료" 표시
            button.textContent = "번역 완료";
            button.disabled = true;
        }
    }
});

async function translateMessage(message) {
    try {
        const response = await fetch("/translate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: message }) // targetLanguage 제거
        });

        if (response.ok) {
            const data = await response.json();
            return data.translatedText; // 번역된 텍스트 반환
        } else {
            console.error("Translation failed");
            return null;
        }
    } catch (error) {
        console.error("Error during translation:", error);
        return null;
    }
}


window.addEventListener("beforeunload", () => {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close();
    }
});