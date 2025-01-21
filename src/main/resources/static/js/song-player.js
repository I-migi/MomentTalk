let player;

// YouTube API 로드 후 호출되는 함수
function onYouTubeIframeAPIReady() {
    player = new YT.Player('player', {
        height: '360', // 초기 화면 크기
        width: '640',
        videoId: '', // 초기화 시 비어 있음
        events: {
            onReady: onPlayerReady,
            onStateChange: onPlayerStateChange,
        },
    });
}

function onPlayerReady(event) {
    console.log('YouTube Player is ready.');
}

// 플레이어 상태 변경 이벤트
function onPlayerStateChange(event) {
    if (event.data === YT.PlayerState.ENDED) {
        console.log('Song has ended.');
    }
}

// 노래 재생
function playSong(videoId, startSeconds, duration) {
    if (!player) {
        alert('YouTube Player is not ready yet.');
        return;
    }

    if (!videoId || !startSeconds || !duration) {
        alert('Please provide all required inputs.');
        return;
    }

    player.loadVideoById({
        videoId: videoId,
        startSeconds: parseFloat(startSeconds),
        endSeconds: parseFloat(startSeconds) + parseFloat(duration),
    });

    player.playVideo();
}

// 일시정지
function pauseSong() {
    if (!player) {
        alert('YouTube Player is not ready yet.');
        return;
    }
    player.pauseVideo();
}

// 버튼 및 입력 처리
document.getElementById('playSong').addEventListener('click', () => {
    const videoId = document.getElementById('songId').value.trim();
    const startSeconds = document.getElementById('startSeconds').value.trim();
    const duration = document.getElementById('duration').value.trim();

    playSong(videoId, startSeconds, duration);
});

document.getElementById('pauseSong').addEventListener('click', pauseSong);