const statusDiv = document.getElementById("status");
const chatBox = document.getElementById("chatBox");
const chatMessages = document.getElementById("chatMessages");
const messageInput = document.getElementById("messageInput");
const sendButton = document.getElementById("sendButton");
const connectButton = document.getElementById("connectButton");

const backButton = document.getElementById("backButton");

backButton.addEventListener("click", () => {
    resetUI();
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close(); // Close the WebSocket connection on exit
    }
});




let socket = null;

// Initialize WebSocket
function initWebSocket() {
    socket = new WebSocket("ws://localhost:8080/ws/connect");
    // socket = new WebSocket("ws://192.168.129.100:8080/ws/connect");

    socket.onopen = () => {
        statusDiv.textContent = "새로운 친구를 찾는 중...";
    };

    socket.onmessage = (event) => {
        if (event.data.startsWith("MATCH_SUCCESS:")) {
            const opponentUser = event.data.split(":")[1];
            statusDiv.style.display = "none";
            connectButton.style.display = "none";
            chatBox.style.display = "flex";
        } else {
            const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

            const messageDiv = document.createElement("div");
            messageDiv.className = "message-received";

            messageDiv.innerHTML = `<span>${event.data}</span>
                                <button class="translate-button" data-message="${event.data}">번역</button>
                                <span class="message-time">${currentTime}</span>`;
            chatMessages.appendChild(messageDiv);
            chatMessages.scrollTop = chatMessages.scrollHeight;
        }
    };


    socket.onclose = () => {
        console.log("WebSocket closed");
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

    if (message && socket && socket.readyState === WebSocket.OPEN) {
        socket.send(message);

        const messageDiv = document.createElement("div");
        messageDiv.className = "message-sent";

        const currentTime = new Date();
        const timeString = currentTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        messageDiv.setAttribute("data-timestamp", currentTime.toISOString()); // 시간 저장
        messageDiv.innerHTML = `<span>You: ${message}</span> <span class="message-time">${timeString}</span>`;
        chatMessages.appendChild(messageDiv);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        messageInput.value = "";
    } else {
        console.error("WebSocket is not open or message is empty.");
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