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
                groupName.textContent = room.name; // 방 이름 표시

                const joinButton = document.createElement('button');
                joinButton.textContent = 'Join';
                joinButton.onclick = () => {
                    alert(`Joining group: ${room.name}`);
                    // Join 그룹 로직 추가
                };

                groupItem.appendChild(groupName);
                groupItem.appendChild(joinButton);
                groupList.appendChild(groupItem);
            });
        })
        .catch(error => console.error('Error fetching group chat rooms:', error));
}

// Create a new group
document.getElementById('createGroup').addEventListener('click', () => {
    const groupName = document.getElementById('groupName').value;
    if (groupName.trim() === '') {
        alert('Group name cannot be empty!');
        return;
    }

    fetch('/group-chat/rooms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: groupName })
    })
        .then(response => response.json())
        .then(data => {
            alert(`${data.message}: ${data.name}`);
            fetchGroupChatRooms(); // Refresh the group list after creating a group
        })
        .catch(error => console.error('Error creating group:', error));
});

// Automatically fetch group chat rooms on page load
window.onload = fetchGroupChatRooms;