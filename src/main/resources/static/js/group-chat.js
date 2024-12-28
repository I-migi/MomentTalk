function fetchGroupChatRooms() {
    fetch('/group-chat/rooms')
        .then(response => response.json())
        .then(data => {
            const groupList = document.getElementById('groupList');
            groupList.innerHTML = ''; // Clear current group list

            Object.entries(data).forEach(([key, value]) => {
                const groupItem = document.createElement('div');
                groupItem.className = 'group-item';

                const groupName = document.createElement('span');
                groupName.textContent = value;

                const joinButton = document.createElement('button');
                joinButton.textContent = 'Join';
                joinButton.onclick = () => {
                    alert(`Joining group: ${value}`);
                    // Add your join group logic here
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