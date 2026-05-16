// ========================================
// 风不止聊天 - 主应用逻辑
// ========================================

// 全局状态
let currentUser = null;
let currentChat = null;
let friends = [];
let groups = [];
let conversations = [];
let friendInfoCache = {}; // 缓存好友详细信息
let pendingMessages = new Map(); // 待确认的消息 {msgId: {message, timeout}}

// 常用表情列表
const EMOJI_LIST = [
    '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂',
    '🙂', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗',
    '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭',
    '🤫', '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏',
    '😒', '🙄', '😬', '🤥', '😌', '😔', '😪', '🤤',
    '😴', '😷', '🤒', '🤕', '🤢', '🤮', '🤧', '🥵',
    '🥶', '🥴', '😵', '🤯', '🤠', '🥳', '😎', '🤓',
    '🧐', '😕', '😟', '🙁', '😮', '😯', '😲', '😳',
    '🥺', '😦', '😧', '😨', '😰', '😥', '😢', '😭',
    '😱', '😖', '😣', '😞', '😓', '😩', '😫', '🥱',
    '😤', '😡', '😠', '🤬', '👋', '🤚', '🖐', '✋',
    '👍', '👎', '👊', '✊', '🤛', '🤜', '👏', '🙌',
    '👐', '🤲', '🤝', '🙏', '💪', '❤', '💔', '❣️',
    '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟',
    '⭐', '🌟', '✨', '⚡', '🔥', '💧', '🌊', '🎉',
    '🎊', '🎈', '🎁', '🏆', '🥇', '🥈', '🥉', '🎪',
    '🌈', '☀️', '🌤', '⛅', '☁️', '🌧', '⛈', '🌩',
    '🌪', '🌫', '🌬', '🌙', '🌚', '🌛', '🌜', '💤',
    '🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓',
    '🫐', '🍈', '🍍', '🥭', '🥝', '🍅', '🍆', '🥑',
    '🥦', '🌽', '🌶', '🫑', '🥒', '🥜', '🍞', '🥐',
    '🥨', '🥯', '🧀', '🥚', '🍳', '🥓', '🥩', '🍔',
    '🍟', '🍕', '🌭', '🌮', '🌮', '🌯', '🥙', '🍖',
    '🍗', '🥩', '🍲', '🍱', '🍘', '🍙', '🍚', '🍛',
    '🍜', '🍝', '🍠', '🍢', '🍣', '🍤', '🍥', '🥮',
    '🥟', '🥠', '🥡', '🍦', '🍧', '🍨', '🍩', '🍪',
    '🎂', '🍰', '🧁', '🥧', '🍫', '🍬', '🍭', '🍮',
    '🍯', '🍼', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼',
    '🐨', '🐯', '🦁', '🐮', '🐷', '🐸', '🐵', '🙈',
    '🙉', '🙊', '🐦', '🐤', '🦆', '🦉', '🦇', '🐺',
    '🐗', '🐴', '🦄', '🐝', '🐛', '🦋', '🐌', '🐞'
];

// ========================================
// 页面初始化
// ========================================
document.addEventListener('DOMContentLoaded', () => {
    console.log('应用启动');

    // 检查是否已登录
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('user');

    if (token && userData) {
        currentUser = JSON.parse(userData);
        showChatPage();
        initializeChat();
    } else {
        showAuthPage();
    }

    // 注册WebSocket消息处理器
    wsManager.onMessage(handleWebSocketMessage);

    // 初始化表情
    initEmojiPicker();
});

// ========================================
// 认证相关功能
// ========================================

// 显示登录页面
function showAuthPage() {
    document.getElementById('auth-page').classList.add('active');
    document.getElementById('chat-page').classList.remove('active');
}

// 显示聊天页面
function showChatPage() {
    document.getElementById('auth-page').classList.remove('active');
    document.getElementById('chat-page').classList.add('active');

    if (currentUser) {
        document.getElementById('current-username').textContent = currentUser.nickname || currentUser.username;
        // 更新头像
        updateUserAvatar();
    }
}

// 更新用户头像显示
function updateUserAvatar() {
    const avatarImg = document.getElementById('current-user-avatar-img');
    const avatarText = document.getElementById('current-user-avatar-text');
    
    if (currentUser && currentUser.avatar) {
        // 将相对路径转换为完整URL
        const avatarUrl = currentUser.avatar.startsWith('http') ? currentUser.avatar : `${API_BASE_URL}${currentUser.avatar}`;
        avatarImg.src = avatarUrl;
        avatarImg.style.display = 'block';
        avatarText.style.display = 'none';
    } else {
        avatarImg.style.display = 'none';
        avatarText.style.display = 'block';
        avatarText.textContent = (currentUser?.nickname || currentUser?.username || '?')[0];
    }
}

// 切换到注册表单
function showRegister() {
    document.getElementById('login-form').classList.remove('active');
    document.getElementById('register-form').classList.add('active');
}

// 切换到登录表单
function showLogin() {
    document.getElementById('register-form').classList.remove('active');
    document.getElementById('login-form').classList.add('active');
}

// 处理登录
async function handleLogin() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;

    if (!username || !password) {
        alert('请输入用户名和密码');
        return;
    }

    try {
        const response = await api.login(username, password);
        console.log('🔐 登录响应:', response);

        if (response.code === 200 && response.data) {
            currentUser = response.data;
            const token = response.data.token;
            console.log('🔑 Token:', token);
            
            if (!token) {
                console.error('❌ 后端未返回 Token！');
                alert('登录失败：服务器未返回认证令牌');
                return;
            }
            
            api.setToken(token);
            localStorage.setItem('user', JSON.stringify(currentUser));
            
            console.log('✅ 登录成功，Token 已保存');
            console.log('📦 LocalStorage Token:', localStorage.getItem('token'));

            alert('登录成功!');
            showChatPage();
            initializeChat();
        } else {
            alert(response.message || '登录失败');
        }
    } catch (error) {
        console.error('登录错误:', error);
        alert('登录失败,请检查网络连接');
    }
}

// 处理注册
async function handleRegister() {
    const username = document.getElementById('reg-username').value.trim();
    const nickname = document.getElementById('reg-nickname').value.trim();
    const password = document.getElementById('reg-password').value;
    const confirmPassword = document.getElementById('reg-confirm-password').value;

    if (!username || !password) {
        alert('请填写必填项');
        return;
    }

    if (password !== confirmPassword) {
        alert('两次输入的密码不一致');
        return;
    }

    try {
        const response = await api.register(username, nickname || username, password);

        if (response.code === 200) {
            alert('注册成功!请登录');
            showLogin();
        } else {
            alert(response.message || '注册失败');
        }
    } catch (error) {
        console.error('注册错误:', error);
        alert('注册失败,请检查网络连接');
    }
}

// 退出登录
function logout() {
    if (confirm('确定要退出登录吗?')) {
        wsManager.disconnect();
        api.clearToken();
        localStorage.removeItem('user');
        currentUser = null;
        currentChat = null;
        showAuthPage();
    }
}

// ========================================
// 聊天初始化
// ========================================

async function initializeChat() {
    if (!currentUser) return;

    // 连接WebSocket
    wsManager.connect(currentUser.userId);

    // 加载数据
    await loadFriends();
    await loadGroups();
    await loadConversations();
}

// 加载好友列表
async function loadFriends() {
    try {
        const response = await api.getFriends();
        if (response.code === 200) {
            friends = response.data || [];
            renderFriendsList();
        }
    } catch (error) {
        console.error('加载好友列表失败:', error);
    }
}

// 渲染好友列表
function renderFriendsList() {
    const container = document.getElementById('friends-list');

    if (friends.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="padding: 40px 20px; text-align: center;">
                <div class="icon">👥</div>
                <h3>暂无联系人</h3>
                <p>点击右上角 ➕ 添加朋友<br>开始你的聊天之旅</p>
                <button onclick="showAddFriendModal()" class="btn-primary" style="margin-top: 20px; padding: 12px 24px; width: auto; display: inline-block;">➕ 添加朋友</button>
            </div>
        `;
        return;
    }

    container.innerHTML = friends.map(friend => {
        const conv = conversations.find(c => c.targetId === friend.userId);
        const unreadCount = conv ? conv.unreadCount : 0;
        const unreadBadge = unreadCount > 0 ? `<span class="unread-badge">${unreadCount > 99 ? '99+' : unreadCount}</span>` : '';
        const displayName = friend.remark || friend.nickname || friend.username;
        const avatarText = (displayName || '?')[0];
        
        // 处理头像显示 - 添加容错处理
        let avatarHtml;
        if (friend.avatar) {
            // 使用完整URL，添加onerror回退
            const avatarUrl = friend.avatar.startsWith('http') ? friend.avatar : `${API_BASE_URL}${friend.avatar}`;
            avatarHtml = `<div class="contact-avatar" style="background-image: url('${avatarUrl}'); background-size: cover; background-position: center;" onerror="this.style.backgroundImage='none';this.style.display='flex';this.style.alignItems='center';this.style.justifyContent='center';this.style.background='linear-gradient(135deg, #667eea 0%, #764ba2 100%)';"></div>`;
        } else {
            avatarHtml = `<div class="contact-avatar">${avatarText}</div>`;
        }

        return `
        <div class="contact-item" onclick="selectChat('friend', ${friend.userId}, '${escapeHtml(displayName)}')">
            ${avatarHtml}
            <div class="contact-info">
                <div class="contact-name">
                    ${escapeHtml(displayName)}
                    ${friend.remark ? `<small style="color: #999; font-size: 12px;">(${escapeHtml(friend.nickname || friend.username)})</small>` : ''}
                </div>
                <div class="contact-last-msg">点击开始聊天</div>
            </div>
            ${unreadBadge}
            <div class="contact-actions" onclick="event.stopPropagation(); showFriendContextMenu(${friend.userId}, '${escapeHtml(displayName)}', '${escapeHtml(friend.nickname || friend.username)}', '${escapeHtml(friend.remark || '')}')">
                <span class="action-icon">⋮</span>
            </div>
        </div>
    `}).join('');
}

// 加载群聊列表
async function loadGroups() {
    try {
        const response = await api.getGroups(currentUser?.userId);
        if (response.code === 200) {
            groups = response.data || [];
            renderGroupsList();
        }
    } catch (error) {
        console.error('加载群聊列表失败:', error);
    }
}

// 渲染群聊列表
function renderGroupsList() {
    const container = document.getElementById('groups-list');
    const createBtnContainer = document.getElementById('create-group-btn-container');

    if (groups.length === 0) {
        // 保留创建按钮,只替换空状态提示
        createBtnContainer.style.display = 'block';
        const emptyStateHtml = `
            <div class="empty-state" style="padding: 40px 20px; text-align: center;">
                <div class="icon">👥</div>
                <h3>暂无群聊</h3>
                <p>点击下方按钮创建群聊<br>与好友一起畅聊</p>
            </div>
        `;
        // 移除旧的群聊列表项，保留创建按钮
        const oldItems = container.querySelectorAll('.contact-item');
        oldItems.forEach(item => item.remove());
        
        // 添加空状态提示（如果不存在）
        if (!container.querySelector('.empty-state')) {
            container.insertAdjacentHTML('beforeend', emptyStateHtml);
        }
        return;
    }

    // 始终显示创建按钮
    createBtnContainer.style.display = 'block';
    
    // 移除空状态提示
    const emptyState = container.querySelector('.empty-state');
    if (emptyState) emptyState.remove();

    // 移除旧的群聊列表项
    const oldItems = container.querySelectorAll('.contact-item');
    oldItems.forEach(item => item.remove());

    // 添加新的群聊列表项
    const groupsHtml = groups.map(group => {
        // 处理群头像显示 - 添加容错处理
        let avatarHtml;
        if (group.avatar) {
            const avatarUrl = group.avatar.startsWith('http') ? group.avatar : `${API_BASE_URL}${group.avatar}`;
            avatarHtml = `<div class="contact-avatar" style="background-image: url('${avatarUrl}'); background-size: cover; background-position: center;" onerror="this.style.backgroundImage='none';this.style.display='flex';this.style.alignItems='center';this.style.justifyContent='center';this.style.background='linear-gradient(135deg, #fa709a 0%, #fee140 100%)';"></div>`;
        } else {
            avatarHtml = `<div class="contact-avatar" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">👥</div>`;
        }
        
        return `
        <div class="contact-item" onclick="selectChat('group', ${group.groupId}, '${escapeHtml(group.groupName)}')">
            ${avatarHtml}
            <div class="contact-info">
                <div class="contact-name">${escapeHtml(group.groupName)}</div>
                <div class="contact-last-msg">${group.memberCount || 0}人</div>
            </div>
        </div>
    `}).join('');
    
    container.insertAdjacentHTML('beforeend', groupsHtml);
}

// 加载会话列表
async function loadConversations() {
    try {
        const response = await api.getConversations(currentUser?.userId);
        if (response.code === 200) {
            conversations = response.data || [];
        }
    } catch (error) {
        console.error('加载会话列表失败:', error);
    }
}

// ========================================
// Emoji选择器功能
// ========================================

// 初始化Emoji选择器
function initEmojiPicker() {
    const emojiGrid = document.getElementById('emoji-grid');
    if (!emojiGrid) return;

    emojiGrid.innerHTML = '';
    EMOJI_LIST.forEach(emoji => {
        if (!emoji) return;

        const btn = document.createElement('button');
        btn.className = 'emoji-btn';
        btn.textContent = emoji;
        btn.onclick = () => insertEmoji(emoji);
        emojiGrid.appendChild(btn);
    });
}

// 切换Emoji选择器显示/隐藏
function toggleEmojiPicker() {
    const picker = document.getElementById('emoji-picker');
    if (!picker) return;

    picker.style.display = picker.style.display === 'none' ? 'block' : 'none';
}

// 插入Emoji到输入框
function insertEmoji(emoji) {
    const input = document.getElementById('message-input');
    if (!input) return;

    const startPos = input.selectionStart;
    const endPos = input.selectionEnd;
    const text = input.value;

    input.value = text.substring(0, startPos) + emoji + text.substring(endPos);
    input.focus();
    input.selectionStart = input.selectionEnd = startPos + emoji.length;

    const picker = document.getElementById('emoji-picker');
    if (picker) picker.style.display = 'none';
}

// 点击页面其他地方关闭Emoji选择器
document.addEventListener('click', (e) => {
    const picker = document.getElementById('emoji-picker');
    const emojiBtn = document.querySelector('[title="表情"]');

    if (picker && !picker.contains(e.target) && e.target !== emojiBtn) {
        picker.style.display = 'none';
    }
});

// ========================================
// 聊天功能
// ========================================

/** 解析后端 LocalDateTime.toString() / ISO 字符串，避免 Invalid Date 导致时间显示错误 */
function parseChatTimestamp(raw) {
    if (raw == null || raw === '') return new Date();
    const s = String(raw).trim();
    let d = new Date(s);
    if (!Number.isNaN(d.getTime())) return d;
    const s2 = s.replace(' ', 'T');
    d = new Date(s2);
    if (!Number.isNaN(d.getTime())) return d;
    return new Date();
}

/** 同步服务端与侧边栏未读角标（当前正在聊的会话也要清，否则会积压到下次刷新才出现） */
async function clearUnreadForContact(targetId, type) {
    try {
        const response = await api.clearUnreadCount(currentUser.userId, targetId, type);
        if (response.code === 200) {
            const conv = conversations.find(c => c.targetId === targetId && c.type === type);
            if (conv) {
                conv.unreadCount = 0;
                renderFriendsList();
            }
        }
    } catch (error) {
        console.error('清除未读数失败:', error);
    }
}

// 选择聊天对象
async function selectChat(type, targetId, name) {
    currentChat = { type, targetId, name };

    // 🔧 关键修复：获取对方或群聊的头像信息
    if (type === 'friend') {
        // 单聊：从好友列表中获取头像
        const friend = friends.find(f => f.userId === targetId);
        if (friend) {
            currentChat.avatar = friend.avatar; // 保存头像URL
            console.log('🔍 单聊 - 获取到好友头像:', friend.avatar);
        }
    } else if (type === 'group') {
        // 群聊：从群组列表中获取群头像
        const group = groups.find(g => g.groupId === targetId);
        if (group) {
            currentChat.avatar = group.avatar; // 保存群头像URL
            console.log('🔍 群聊 - 获取到群头像:', group.avatar);
        }
    }

    await clearUnreadForContact(targetId, type);

    // 更新UI选中状态
    document.querySelectorAll('.contact-item').forEach(item => item.classList.remove('active'));
    if (event && event.currentTarget) {
        event.currentTarget.classList.add('active');
    }

    document.getElementById('no-chat-selected').style.display = 'none';
    document.getElementById('chat-window').classList.add('active');
    document.getElementById('chat-name').textContent = name;
    
    // 显示/隐藏群设置按钮
    const groupSettingsBtn = document.getElementById('btn-group-settings');
    if (type === 'group') {
        groupSettingsBtn.style.display = 'block';
    } else {
        groupSettingsBtn.style.display = 'none';
    }
    
    // 🔧 更新聊天顶部头像 - 直接使用currentChat.avatar
    const chatAvatar = document.getElementById('chat-avatar');
    if (currentChat.avatar) {
        const avatarUrl = currentChat.avatar.startsWith('http') ? currentChat.avatar : `${API_BASE_URL}${currentChat.avatar}`;
        console.log('🔧 设置聊天顶部头像:', avatarUrl);
        chatAvatar.style.backgroundImage = `url('${avatarUrl}')`;
        chatAvatar.style.backgroundSize = 'cover';
        chatAvatar.style.backgroundPosition = 'center';
        chatAvatar.textContent = '';
    } else {
        chatAvatar.style.backgroundImage = '';
        chatAvatar.textContent = name[0];
    }
    chatAvatar.style.cursor = 'pointer';

    if (window.innerWidth <= 768) {
        document.getElementById('sidebar').classList.remove('open');
    }

    loadMessages();
}

// 触发文件选择
function triggerFileSelect() {
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = 'image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt,.zip,.rar';
    fileInput.onchange = handleFileSelect;
    fileInput.click();
}

// 处理文件选择
async function handleFileSelect(event) {
    const file = event.target.files[0];
    if (!file) return;

    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
        alert('文件大小不能超过10MB');
        return;
    }

    if (!currentChat) {
        alert('请先选择一个聊天对象');
        return;
    }

    try {
        addSystemMessage('正在上传文件...');

        const response = await api.uploadFile(file);

        if (response.code === 200 && response.data) {
            const fileId = response.data.fileId;
            const fileName = response.data.filename;
            const fileSize = parseInt(response.data.size) || 0;
            const fileUrl = response.data.url;

            // 🔥 生成唯一消息ID
            const msgId = generateMessageId();
            const timestamp = new Date().toISOString();

            const message = {
                type: 'chat',
                msgId: msgId, // 🔥 添加消息ID用于确认
                msgType: 'file',
                content: fileName,
                extra: fileId,
                fileSize: fileSize,
                senderId: currentUser.userId,
                receiverId: currentChat.targetId,
                conversationType: currentChat.type,
                timestamp: timestamp
            };

            if (wsManager.send(message)) {
                // 🔥 将文件消息也加入待确认列表
                savePendingMessage(msgId, {
                    msgId: msgId,
                    senderId: currentUser.userId,
                    content: fileName,
                    msgType: 'file',
                    extra: fileId,
                    fileSize: fileSize,
                    timestamp: timestamp,
                    sender: 'me',
                    status: 'sending'
                });
                
                addMessageToUI({
                    senderId: currentUser.userId,
                    content: fileName,
                    msgType: 'file',
                    extra: fileId,
                    fileSize: fileSize,
                    timestamp: timestamp,
                    sender: 'me',
                    msgId: msgId,
                    status: 'sending'
                });
                
                // 🔥 设置超时
                setupMessageTimeout(msgId);
            } else {
                alert('发送失败，请检查网络连接');
            }
        } else {
            alert(response.message || '文件上传失败');
        }
    } catch (error) {
        console.error('文件上传错误:', error);
        alert('文件上传失败，请重试');
    }
}

// 加载消息
async function loadMessages() {
    if (!currentChat || !currentChat.targetId) {
        console.error('无效的聊天对象或目标ID缺失');
        return;
    }

    const messageList = document.getElementById('message-list');
    messageList.innerHTML = '<div class="empty-state"><p>加载中...</p></div>';

    try {
        // 群聊直接用groupId作为convId，单聊才用计算公式
        let convId;
        if (currentChat.type === 'group') {
            convId = currentChat.targetId;
            console.log('🔍 群聊消息 - groupId:', convId);
        } else {
            convId = Math.min(currentUser.userId, currentChat.targetId) * 100000 + Math.max(currentUser.userId, currentChat.targetId);
            console.log('🔍 单聊消息 - convId:', convId);
        }

        if (isNaN(convId) || convId === 0) {
            messageList.innerHTML = '<div class="empty-state"><p>加载失败：ID错误</p></div>';
            return;
        }

        // 🔥 直接从本地缓存加载历史消息（上线时后端已通过 WebSocket 推送离线消息）
        console.log('🔍 开始加载本地缓存, convId:', convId, 'userId:', currentUser.userId);
        const localMessages = getLocalMessages(convId);
        console.log('📦 本地缓存消息数量:', localMessages.length);
        
        messageList.innerHTML = '';
        
        if (localMessages.length === 0) {
            addSystemMessage(`这是与 ${currentChat.name} 的开始`);
        } else {
            // 🔥 性能优化：使用 DocumentFragment 批量插入
            const fragment = document.createDocumentFragment();
            localMessages.forEach(msg => {
                const msgEl = document.createElement('div');
                const msgSenderId = typeof msg.senderId === 'string' ? parseInt(msg.senderId) : msg.senderId;
                const currentUid = typeof currentUser?.userId === 'string' ? parseInt(currentUser.userId) : currentUser?.userId;
                const isMe = msg.sender === 'me' || msgSenderId === currentUid;
                
                msgEl.className = `message ${isMe ? 'me' : 'other'}`;
                if (msg.msgId) msgEl.setAttribute('data-msg-id', msg.msgId);

                const time = parseChatTimestamp(msg.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
                const contentHtml = msg.msgType === 'file' ? `[文件] ${msg.content}` : escapeHtml(msg.content);
                
                // 🔥 获取头像样式和文本
                const avatarStyle = getMessageAvatarStyle(msg, isMe);
                const avatarText = getMessageAvatarText(msg, isMe);
                const avatarHtml = `<div class="message-avatar" style="${avatarStyle}">${avatarText}</div>`;
                
                msgEl.innerHTML = `
                    ${avatarHtml}
                    <div class="message-content">
                        ${!isMe && currentChat?.type === 'group' ? `<div class="message-sender">${getSenderName(msg.senderId)}</div>` : ''}
                        <div class="message-bubble">${contentHtml}</div>
                        <div class="message-time">${time}</div>
                    </div>
                `;
                fragment.appendChild(msgEl);
            });
            messageList.appendChild(fragment);
            messageList.scrollTop = messageList.scrollHeight;
        }
    } catch (error) {
        console.error('加载消息失败:', error);
        messageList.innerHTML = '<div class="empty-state"><p>加载失败</p></div>';
    }
}

// 🔥 获取本地缓存的消息
function getLocalMessages(convId) {
    try {
        const cacheKey = `chat_history_${convId}_${currentUser.userId}`;
        console.log('🔍 尝试加载缓存, key:', cacheKey);
        const cached = localStorage.getItem(cacheKey);
        console.log('📦 缓存原始数据:', cached);
        if (cached) {
            const messages = JSON.parse(cached);
            console.log('✅ 解析缓存成功, 消息数量:', messages.length);
            return Array.isArray(messages) ? messages : [];
        } else {
            console.log('⚠️ 缓存不存在');
        }
    } catch (error) {
        console.error('读取本地消息缓存失败:', error);
    }
    return [];
}

// 🔥 保存消息到本地缓存
function saveMessagesToLocal(convId, messages) {
    try {
        const cacheKey = `chat_history_${convId}_${currentUser.userId}`;
        console.log('💾 准备保存消息到缓存, key:', cacheKey, '消息数量:', messages.length);
        
        // 🔥 保护机制：如果消息列表为空，不覆盖缓存（防止意外清空）
        if (!messages || messages.length === 0) {
            console.warn('⚠️ 消息列表为空，跳过保存以保护现有缓存');
            return;
        }
        
        // 转换消息格式以适应本地缓存
        const formattedMessages = messages.map(msg => {
            // 🔥 类型安全比较：统一转为数字类型
            const msgSenderId = typeof msg.senderId === 'string' ? parseInt(msg.senderId) : msg.senderId;
            const currentUid = typeof currentUser?.userId === 'string' ? parseInt(currentUser.userId) : currentUser?.userId;
            
            return {
                senderId: msg.senderId,
                content: msg.content,
                msgType: msg.msgType,
                extra: msg.extra,
                fileSize: msg.fileSize,
                timestamp: msg.sendTime || msg.timestamp, // 🔧 兼容两种时间字段
                sender: msgSenderId === currentUid ? 'me' : 'other'
            };
        });
        localStorage.setItem(cacheKey, JSON.stringify(formattedMessages));
        console.log('✅ 消息已保存到缓存, key:', cacheKey);
    } catch (error) {
        console.error('保存本地消息缓存失败:', error);
    }
}

// 🔥 新增：保存单个接收到的消息到本地缓存
function saveReceivedMessageToLocal(messageData) {
    try {
        // 🔥 修复：即使没有当前聊天窗口，也要保存消息
        const senderId = typeof messageData.senderId === 'string' ? parseInt(messageData.senderId) : messageData.senderId;
        
        let convId;
        if (messageData.conversationType === 'group') {
            // 群聊：直接使用 messageData.convId
            convId = messageData.convId;
        } else {
            // 🔥 单聊：使用 senderId 和当前用户ID计算 convId
            convId = Math.min(currentUser.userId, senderId) * 100000 + Math.max(currentUser.userId, senderId);
        }
        
        const cacheKey = `chat_history_${convId}_${currentUser.userId}`;
        console.log('💾 准备保存接收消息到缓存, key:', cacheKey, 'convId:', convId);
        
        // 获取现有缓存
        let cachedMessages = [];
        try {
            const cached = localStorage.getItem(cacheKey);
            if (cached) {
                cachedMessages = JSON.parse(cached);
            }
        } catch (e) {
            console.warn('解析本地缓存失败，将创建新缓存', e);
        }
        
        // 🔥 去重检查：避免重复保存同一条消息
        const existingMsg = cachedMessages.find(m => {
            // 优先用 msgId 比较
            if (m.msgId && messageData.msgId && m.msgId === messageData.msgId) {
                return true;
            }
            // 其次用 timestamp + content 比较
            const mTime = m.timestamp || m.sendTime;
            const newTime = messageData.timestamp || messageData.sendTime;
            if (mTime && newTime && mTime === newTime && m.content === messageData.content) {
                return true;
            }
            return false;
        });
        
        if (existingMsg) {
            console.log('⏭️ 消息已存在，跳过保存:', messageData.msgId || messageData.timestamp);
            return;
        }
        
        // 添加新消息
        const newMessage = {
            senderId: messageData.senderId,
            content: messageData.content,
            msgType: messageData.msgType || 'text',
            extra: messageData.extra,
            fileSize: messageData.fileSize,
            timestamp: messageData.timestamp || new Date().toISOString(),
            sender: 'other',
            msgId: messageData.msgId  // 🔥 保存 msgId 用于后续去重
        };
        
        cachedMessages.push(newMessage);
        
        // 限制缓存数量（最多100条）
        if (cachedMessages.length > 100) {
            cachedMessages = cachedMessages.slice(-100);
        }
        
        localStorage.setItem(cacheKey, JSON.stringify(cachedMessages));
        console.log('✅ 消息已保存到本地缓存:', cacheKey);
    } catch (error) {
        console.error('❌ 保存接收消息到本地缓存失败:', error);
    }
}

// 发送消息
function sendMessage() {
    const input = document.getElementById('message-input');
    const content = input.value.trim();

    if (!content) return;
    
    if (!currentChat) {
        alert('请先选择一个聊天对象');
        return;
    }

    // 🔥 生成唯一消息ID
    const msgId = generateMessageId();
    const timestamp = new Date().toISOString();

    const message = {
        type: 'chat',
        msgId: msgId, // 添加消息ID
        content: content,
        senderId: currentUser.userId,
        receiverId: currentChat.targetId,
        conversationType: currentChat.type,
        timestamp: timestamp
    };

    if (wsManager.send(message)) {
        // 🔥 将消息存储到浏览器缓存并显示为“发送中”
        savePendingMessage(msgId, {
            msgId: msgId,
            senderId: currentUser.userId,
            content: content,
            timestamp: timestamp,
            sender: 'me',
            status: 'sending' // 发送中状态
        });
            
        addMessageToUI({
            senderId: currentUser.userId,
            content: content,
            timestamp: timestamp,
            sender: 'me',
            msgId: msgId,
            status: 'sending'
        });
    
        input.value = '';
        input.style.height = 'auto';
            
        // 🔥 关键修复：立即保存消息到本地缓存，防止刷新丢失
        try {
            let convId;
            if (currentChat.type === 'group') {
                convId = currentChat.targetId;
            } else {
                convId = Math.min(currentUser.userId, currentChat.targetId) * 100000 + Math.max(currentUser.userId, currentChat.targetId);
            }
                
            const cacheKey = `chat_history_${convId}_${currentUser.userId}`;
            let cachedMessages = [];
            try {
                const cached = localStorage.getItem(cacheKey);
                if (cached) {
                    cachedMessages = JSON.parse(cached);
                }
            } catch (e) {
                console.warn('解析本地缓存失败', e);
            }
                
            // 添加新消息
            cachedMessages.push({
                senderId: currentUser.userId,
                content: content,
                msgType: 'text',
                timestamp: timestamp,
                sender: 'me'
            });
                
            // 限制缓存数量
            if (cachedMessages.length > 100) {
                cachedMessages = cachedMessages.slice(-100);
            }
                
            localStorage.setItem(cacheKey, JSON.stringify(cachedMessages));
            console.log('✅ 发送消息已保存到本地缓存:', cacheKey);
        } catch (error) {
            console.error('保存发送消息到缓存失败:', error);
        }
            
        // 🔥 设置10秒超时
        setupMessageTimeout(msgId);
    } else {
        alert('发送失败，请检查网络连接');
    }
}

// 处理输入框键盘事件
function handleInputKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// 自动调整输入框高度
function autoResize(textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
}

// 添加消息到UI
function addMessageToUI(message) {
    const messageList = document.getElementById('message-list');
    
    // 🔧 关键修复：确保类型一致再比较
    const msgSenderId = typeof message.senderId === 'string' ? parseInt(message.senderId) : message.senderId;
    const currentUid = typeof currentUser?.userId === 'string' ? parseInt(currentUser.userId) : currentUser?.userId;
    
    const isMe = message.sender === 'me' || msgSenderId === currentUid;
    
    console.log('🔍 消息归属判断:', {
        'message.sender': message.sender,
        'message.senderId': message.senderId,
        'message.senderId类型': typeof message.senderId,
        'currentUser.userId': currentUser?.userId,
        'currentUser.userId类型': typeof currentUser?.userId,
        'msgSenderId(转换后)': msgSenderId,
        'currentUid(转换后)': currentUid,
        'isMe结果': isMe
    });

    const messageEl = document.createElement('div');
    messageEl.className = `message ${isMe ? 'me' : 'other'}`;
    if (message.msgId) {
        messageEl.setAttribute('data-msg-id', message.msgId);
    }

    const time = parseChatTimestamp(message.timestamp).toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit'
    });

    const msgType = message.msgType || 'text';
    let contentHtml = '';

    if (msgType === 'file') {
        const fileName = message.content;
        const fileId = message.extra;
        const fileUrl = `${API_BASE_URL}/api/file/download/${fileId}`;
        const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(fileName);

        if (isMe) {
            if (isImage) {
                contentHtml = `
                    <div class="message-file">
                        <a href="${fileUrl}" target="_blank" class="image-preview">
                            <img src="${fileUrl}" alt="${fileName}" style="max-width: 200px; max-height: 200px; border-radius: 8px; cursor: pointer;" />
                        </a>
                        <div class="file-name">${escapeHtml(fileName)}</div>
                    </div>
                `;
            } else {
                const fileIcon = getFileIcon(fileName);
                contentHtml = `
                    <div class="message-file">
                        <a href="${fileUrl}" target="_blank" download class="file-link">
                            <span class="file-icon">${fileIcon}</span>
                            <span class="file-name">${escapeHtml(fileName)}</span>
                        </a>
                    </div>
                `;
            }
        } else {
            const fileIcon = getFileIcon(fileName);
            const fileSize = formatFileSize(message.fileSize || 0);
            contentHtml = `
                <div class="message-file pending-file" data-file-id="${fileId}" data-file-name="${escapeHtml(fileName)}" data-file-url="${fileUrl}">
                    <div class="file-info">
                        <span class="file-icon">${fileIcon}</span>
                        <div class="file-details">
                            <div class="file-name">${escapeHtml(fileName)}</div>
                            <div class="file-size">${fileSize}</div>
                        </div>
                    </div>
                    <div class="file-actions">
                        <button class="btn-accept-file" onclick="acceptFile('${fileId}', '${escapeHtml(fileName)}', '${fileUrl}')">接受文件</button>
                    </div>
                </div>
            `;
        }
    } else {
        contentHtml = escapeHtml(message.content);
    }

    const avatarStyle = getMessageAvatarStyle(message, isMe);
    const avatarText = avatarStyle.includes('background-image') ? '' : getMessageAvatarText(message, isMe);
    
    messageEl.innerHTML = `
        <div class="message-avatar" style="${avatarStyle}">${avatarText}</div>
        <div class="message-content">
            ${!isMe && currentChat?.type === 'group' ? `<div class="message-sender">${getSenderName(message.senderId)}</div>` : ''}
            <div class="message-bubble" data-msg-id="${message.msgId || ''}">${contentHtml}${isMe && message.status ? getMessageStatusIcon(message.status) : ''}</div>
            <div class="message-time">${time}</div>
        </div>
    `;

    messageList.appendChild(messageEl);
    messageList.scrollTop = messageList.scrollHeight;
}

// 根据文件扩展名获取图标
function getFileIcon(fileName) {
    const ext = fileName.split('.').pop().toLowerCase();
    const iconMap = {
        'pdf': '📄',
        'doc': '📝',
        'docx': '📝',
        'xls': '📊',
        'xlsx': '📊',
        'ppt': '📽️',
        'pptx': '📽️',
        'txt': '📃',
        'zip': '📦',
        'rar': '📦',
        '7z': '📦'
    };
    return iconMap[ext] || '📎';
}

// 格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '未知大小';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

// 获取消息状态图标
function getMessageStatusIcon(status) {
    if (status === 'sending') {
        return '<span class="msg-status">⏳</span>';
    } else if (status === 'sent') {
        return '<span class="msg-status">✓</span>';
    } else if (status === 'failed') {
        return '<span class="msg-status" style="color: #ff4444;">✗</span>';
    }
    return '';
}

// 接受文件（即下载）
function acceptFile(fileId, fileName, fileUrl) {
    const pendingFileEl = document.querySelector(`.pending-file[data-file-id="${fileId}"]`);
    if (!pendingFileEl) return;

    window.open(fileUrl, '_blank');

    const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(fileName);
    let acceptedHtml = '';

    if (isImage) {
        acceptedHtml = `
            <div class="message-file accepted-file">
                <a href="${fileUrl}" target="_blank" class="image-preview">
                    <img src="${fileUrl}" alt="${fileName}" style="max-width: 200px; max-height: 200px; border-radius: 8px; cursor: pointer;" />
                </a>
                <div class="file-name">${escapeHtml(fileName)}</div>
            </div>
        `;
    } else {
        const fileIcon = getFileIcon(fileName);
        acceptedHtml = `
            <div class="message-file accepted-file">
                <a href="${fileUrl}" target="_blank" download class="file-link">
                    <span class="file-icon">${fileIcon}</span>
                    <span class="file-name">${escapeHtml(fileName)}</span>
                </a>
            </div>
        `;
    }

    pendingFileEl.outerHTML = acceptedHtml;
}

// 显示聊天更多选项
function showChatMore() {
    if (!currentChat) return;
    
    // 创建右键菜单
    const menu = document.createElement('div');
    menu.className = 'context-menu';
    menu.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: white;
        border-radius: 12px;
        box-shadow: 0 8px 32px rgba(0,0,0,0.2);
        padding: 8px 0;
        min-width: 200px;
        z-index: 10000;
    `;
    
    let menuItems = '';
    
    if (currentChat.type === 'friend') {
        // 单聊菜单
        menuItems = `
            <div class="menu-item" onclick="showFriendProfileFromChat(${currentChat.targetId})" style="padding: 12px 20px; cursor: pointer; hover: background: #f5f5f5;">👤 查看资料</div>
            <div class="menu-item" onclick="editFriendRemarkFromChat(${currentChat.targetId})" style="padding: 12px 20px; cursor: pointer;">📝 修改备注</div>
            <div class="menu-item" onclick="clearChatHistory()" style="padding: 12px 20px; cursor: pointer;">🗑️ 清空聊天记录</div>
            <div class="menu-item" onclick="deleteFriendFromChat(${currentChat.targetId}, '${escapeHtml(currentChat.name)}')" style="padding: 12px 20px; cursor: pointer; color: #ff4444;">❌ 删除好友</div>
        `;
    } else if (currentChat.type === 'group') {
        // 群聊菜单
        menuItems = `
            <div class="menu-item" onclick="showGroupMembersModal()" style="padding: 12px 20px; cursor: pointer;">👥 群成员管理</div>
            <div class="menu-item" onclick="clearChatHistory()" style="padding: 12px 20px; cursor: pointer;">🗑️ 清空聊天记录</div>
            <div class="menu-item" onclick="exitGroup()" style="padding: 12px 20px; cursor: pointer; color: #ff4444;">🚪 退出群聊</div>
        `;
    }
    
    menu.innerHTML = menuItems;
    
    // 添加遮罩层
    const overlay = document.createElement('div');
    overlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0,0,0,0.3);
        z-index: 9999;
    `;
    overlay.onclick = () => {
        document.body.removeChild(overlay);
        document.body.removeChild(menu);
    };
    
    document.body.appendChild(overlay);
    document.body.appendChild(menu);
}

// 🔧 从聊天窗口查看好友资料
function showFriendProfileFromChat(friendId) {
    closeChatMore();
    
    const friend = friends.find(f => f.userId === friendId);
    if (!friend) {
        alert('好友信息不存在');
        return;
    }
    
    window.currentViewingFriendId = friendId;
    
    // 填充资料
    const displayNameEl = document.getElementById('friend-profile-display-name');
    const displayIdEl = document.getElementById('friend-profile-display-id');
    const nicknameEl = document.getElementById('friend-profile-nickname');
    const usernameEl = document.getElementById('friend-profile-username');
    const remarkEl = document.getElementById('friend-profile-remark');
    const addTimeEl = document.getElementById('friend-add-time');
    const avatarImg = document.getElementById('friend-profile-avatar-img');
    const avatarText = document.getElementById('friend-profile-avatar-text');
    const btnSendRequest = document.getElementById('btn-send-request');
    const modal = document.getElementById('friend-profile-modal');
    
    if (!displayNameEl || !modal || !btnSendRequest) {
        console.error('❌ 资料弹窗DOM元素不存在');
        return;
    }
    
    displayNameEl.textContent = friend.nickname || friend.username;
    displayIdEl.textContent = friend.userId;
    nicknameEl.value = friend.nickname || '';
    usernameEl.value = friend.username || '';
    remarkEl.value = friend.remark || '';
    addTimeEl.value = '已添加';
    
    // 头像
    if (avatarImg && avatarText) {
        if (friend.avatar) {
            const avatarUrl = friend.avatar.startsWith('http') ? friend.avatar : `${API_BASE_URL}${friend.avatar}`;
            avatarImg.src = avatarUrl;
            avatarImg.style.display = 'block';
            avatarText.style.display = 'none';
        } else {
            avatarImg.style.display = 'none';
            avatarText.style.display = 'block';
            avatarText.textContent = (friend.nickname || friend.username || '?')[0];
        }
    }
    
    // 更新按钮状态（已是好友）
    btnSendRequest.textContent = '✅ 已是好友';
    btnSendRequest.disabled = true;
    btnSendRequest.style.opacity = '0.5';
    
    modal.style.display = 'flex';
}

// 🔧 从聊天窗口修改备注
async function editFriendRemarkFromChat(friendId) {
    closeChatMore();
    const friend = friends.find(f => f.userId === friendId);
    if (!friend) return;
    
    const newRemark = prompt('请输入备注名称:', friend.remark || '');
    if (newRemark === null) return; // 用户取消
    
    try {
        const response = await api.setRemark(friendId, newRemark || null);
        if (response.code === 200) {
            // 更新本地数据
            friend.remark = newRemark || null;
            renderFriendsList();
            
            // 更新聊天顶部名称
            const displayName = newRemark || friend.nickname || friend.username;
            currentChat.name = displayName;
            document.getElementById('chat-name').textContent = displayName;
            
            showNotification('备注修改成功', 'success');
        } else {
            alert(response.message || '修改失败');
        }
    } catch (error) {
        console.error('修改备注错误:', error);
        alert('修改失败，请检查网络连接');
    }
}

// 🔧 清空聊天记录（仅前端UI和本地缓存，不清除后端数据）
function clearChatHistory() {
    closeChatMore();
    
    if (!currentChat) return;
    
    if (!confirm(`确定要清空与 ${currentChat.name} 的聊天记录吗？\n注意：此操作仅清除当前设备的显示，其他设备仍可查看。`)) {
        return;
    }
    
    // 计算convId
    let convId;
    if (currentChat.type === 'group') {
        convId = currentChat.targetId;
    } else {
        convId = Math.min(currentUser.userId, currentChat.targetId) * 100000 + Math.max(currentUser.userId, currentChat.targetId);
    }
    
    // 清空消息列表UI
    const messageList = document.getElementById('message-list');
    messageList.innerHTML = '';
    addSystemMessage(`这是与 ${currentChat.name} 的开始`);
    
    // 🔥 清除本地缓存
    try {
        const cacheKey = `chat_history_${convId}_${currentUser.userId}`;
        localStorage.removeItem(cacheKey);
        console.log('🗑️ 已清除本地缓存:', cacheKey);
    } catch (error) {
        console.error('清除本地缓存失败:', error);
    }
    
    // 清空内存中的待确认消息（如果有）
    pendingMessages.clear();
    
    showNotification('聊天记录已清空', 'success');
    console.log('🗑️ 已清空前端显示和本地缓存（MongoDB数据保留）');
}

// 🔧 从聊天窗口删除好友
async function deleteFriendFromChat(friendId, displayName) {
    closeChatMore();
    
    if (!confirm(`确定要删除好友「${displayName}」吗？`)) {
        return;
    }
    
    try {
        const response = await api.removeFriend(friendId);
        if (response.code === 200) {
            alert('好友已删除');
            
            // 重新加载好友列表
            await loadFriends();
            
            // 关闭聊天窗口
            document.getElementById('chat-window').classList.remove('active');
            document.getElementById('no-chat-selected').style.display = 'block';
            currentChat = null;
        } else {
            alert(response.message || '删除失败');
        }
    } catch (error) {
        console.error('删除好友错误:', error);
        alert('删除失败，请检查网络连接');
    }
}

// 🔧 显示群成员管理模态框
async function showGroupMembersModal() {
    closeChatMore();
    
    if (!currentChat || currentChat.type !== 'group') return;
    
    const groupId = currentChat.targetId;
    
    try {
        const response = await api.getGroupDetail(groupId);
        if (response.code !== 200 || !response.data) {
            alert('获取群信息失败');
            return;
        }
        
        const { group, members } = response.data;
        
        // 检查当前用户是否是管理员或群主
        const currentMember = members.find(m => m.userId === currentUser.userId);
        const isAdmin = currentMember && (currentMember.role === 'owner' || currentMember.role === 'admin');
        const isOwner = currentMember && currentMember.role === 'owner';
        
        // 创建模态框
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.style.display = 'flex';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 600px; max-height: 80vh; overflow-y: auto;">
                <div class="modal-header">
                    <h3>👥 群成员管理 (${members.length}人)</h3>
                    <button onclick="this.closest('.modal').remove()" class="btn-close">×</button>
                </div>
                <div class="modal-body">
                    ${members.map(member => {
                        const friend = friends.find(f => f.userId === member.userId);
                        const displayName = friend ? (friend.remark || friend.nickname || friend.username) : `用户${member.userId}`;
                        const avatarUrl = friend?.avatar ? (friend.avatar.startsWith('http') ? friend.avatar : `${API_BASE_URL}${friend.avatar}`) : '';
                        
                        return `
                            <div class="member-item" style="display: flex; align-items: center; padding: 12px; border-bottom: 1px solid #eee;">
                                <div style="width: 40px; height: 40px; border-radius: 50%; ${avatarUrl ? `background-image: url('${avatarUrl}'); background-size: cover;` : 'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);'} display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; margin-right: 12px;">
                                    ${!avatarUrl ? (displayName[0] || '👤') : ''}
                                </div>
                                <div style="flex: 1;">
                                    <div style="font-weight: 500;">${escapeHtml(displayName)}</div>
                                    <div style="font-size: 12px; color: #999;">${getRoleText(member.role)}</div>
                                </div>
                                ${isAdmin && member.userId !== currentUser.userId ? `
                                    <div style="display: flex; gap: 8px;">
                                        ${isOwner ? `
                                            <select onchange="updateMemberRole(${groupId}, ${member.userId}, this.value)" style="padding: 4px 8px; border: 1px solid #ddd; border-radius: 4px;">
                                                <option value="member" ${member.role === 'member' ? 'selected' : ''}>普通成员</option>
                                                <option value="admin" ${member.role === 'admin' ? 'selected' : ''}>管理员</option>
                                                <option value="owner" ${member.role === 'owner' ? 'selected' : ''}>群主</option>
                                            </select>
                                        ` : `
                                            <select onchange="updateMemberRole(${groupId}, ${member.userId}, this.value)" style="padding: 4px 8px; border: 1px solid #ddd; border-radius: 4px;">
                                                <option value="member" ${member.role === 'member' ? 'selected' : ''}>普通成员</option>
                                                <option value="admin" ${member.role === 'admin' ? 'selected' : ''}>管理员</option>
                                            </select>
                                        `}
                                        <button onclick="removeGroupMember(${groupId}, ${member.userId}, '${escapeHtml(displayName)}')" style="padding: 4px 12px; background: #ff4444; color: white; border: none; border-radius: 4px; cursor: pointer;">移除</button>
                                    </div>
                                ` : ''}
                            </div>
                        `;
                    }).join('')}
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
    } catch (error) {
        console.error('获取群成员失败:', error);
        alert('获取群成员失败');
    }
}

// 🔧 获取职务文本
function getRoleText(role) {
    const roleMap = {
        'owner': '👑 群主',
        'admin': '⭐ 管理员',
        'member': '👤 成员'
    };
    return roleMap[role] || '👤 成员';
}

// 🔧 修改群成员职务
async function updateMemberRole(groupId, userId, newRole) {
    try {
        const response = await api.updateGroupMemberRole(groupId, userId, newRole);
        if (response.code === 200) {
            showNotification('职务修改成功', 'success');
            // 刷新群成员列表
            setTimeout(() => {
                document.querySelector('.modal')?.remove();
                showGroupMembersModal();
            }, 500);
        } else {
            alert(response.message || '修改失败');
        }
    } catch (error) {
        console.error('修改职务错误:', error);
        alert('修改失败，请检查网络连接');
    }
}

// 🔧 移除群成员
async function removeGroupMember(groupId, userId, displayName) {
    if (!confirm(`确定要移除「${displayName}」吗？`)) {
        return;
    }
    
    try {
        const response = await api.removeGroupMember(groupId, userId);
        if (response.code === 200) {
            showNotification('成员已移除', 'success');
            // 刷新群成员列表
            setTimeout(() => {
                document.querySelector('.modal')?.remove();
                showGroupMembersModal();
            }, 500);
        } else {
            alert(response.message || '移除失败');
        }
    } catch (error) {
        console.error('移除成员错误:', error);
        alert('移除失败，请检查网络连接');
    }
}

// 🔧 退出群聊
async function exitGroup() {
    closeChatMore();
    
    if (!confirm(`确定要退出群聊「${currentChat.name}」吗？`)) {
        return;
    }
    
    try {
        const response = await api.removeGroupMember(currentChat.targetId, currentUser.userId);
        if (response.code === 200) {
            alert('已退出群聊');
            
            // 重新加载群列表
            await loadGroups();
            
            // 关闭聊天窗口
            document.getElementById('chat-window').classList.remove('active');
            document.getElementById('no-chat-selected').style.display = 'block';
            currentChat = null;
        } else {
            alert(response.message || '退出失败');
        }
    } catch (error) {
        console.error('退出群聊错误:', error);
        alert('退出失败，请检查网络连接');
    }
}

// 🔧 关闭聊天菜单
function closeChatMore() {
    // 移除所有模态框和遮罩
    document.querySelectorAll('.modal').forEach(m => m.remove());
}

// 添加系统消息
function addSystemMessage(content) {
    const messageList = document.getElementById('message-list');
    const el = document.createElement('div');
    el.className = 'system-message';
    el.style.textAlign = 'center';
    el.style.color = '#999';
    el.style.fontSize = '12px';
    el.style.margin = '8px 0';
    el.textContent = content;
    messageList.appendChild(el);
}

// HTML转义
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 获取发送者名字(群聊时使用)
function getSenderName(senderId) {
    // 首先从好友列表中查找
    const friend = friends.find(f => f.userId === senderId);
    if (friend) {
        return friend.remark || friend.nickname || friend.username || '未知用户';
    }
    
    // 如果是自己
    if (senderId === currentUser?.userId) {
        return currentUser.nickname || currentUser.username || '我';
    }
    
    // 默认返回用户ID
    return `用户${senderId}`;
}

// 🔧 新增：获取消息头像样式
function getMessageAvatarStyle(message, isMe) {
    let avatarUrl = null;
    
    if (isMe) {
        // 自己的头像
        avatarUrl = currentUser?.avatar;
    } else {
        // 对方的头像
        if (currentChat?.type === 'group') {
            // 群聊：需要获取发送者的头像
            const friend = friends.find(f => f.userId === message.senderId);
            if (friend) {
                avatarUrl = friend.avatar;
            }
        } else {
            // 单聊：使用currentChat的头像(即对方头像)
            avatarUrl = currentChat?.avatar;
        }
    }
    
    if (avatarUrl) {
        const fullUrl = avatarUrl.startsWith('http') ? avatarUrl : `${API_BASE_URL}${avatarUrl}`;
        return `background-image: url('${fullUrl}'); background-size: cover; background-position: center;`;
    }
    return '';
}

// 🔧 新增：获取消息头像文本(当没有头像图片时显示)
function getMessageAvatarText(message, isMe) {
    if (isMe) {
        return (currentUser?.nickname || currentUser?.username || '?')[0];
    } else {
        if (currentChat?.type === 'group') {
            // 群聊：显示发送者名字首字母
            const friend = friends.find(f => f.userId === message.senderId);
            if (friend) {
                const name = friend.remark || friend.nickname || friend.username;
                return name ? name[0] : '👤';
            }
            return '👤';
        } else {
            // 单聊：显示对方名字首字母
            return (currentChat?.name || '?')[0];
        }
    }
}

// ========================================
// WebSocket消息处理
// ========================================

function handleWebSocketMessage(type, data) {
    console.log('========== 收到WebSocket消息 ==========');
    console.log('消息类型:', type);
    console.log('消息数据:', JSON.stringify(data, null, 2));
    console.log('当前聊天:', currentChat);
    console.log('=========================================');

    switch (type) {
        case 'connected':
            console.log('WebSocket已连接');
            break;
        case 'message':
            console.log('📨 收到message类型消息，data:', data);
            console.log('  - data.msgId:', data.msgId);
            console.log('  - data.stored:', data.stored);
            console.log('  - data.serverMsgId:', data.serverMsgId);
            
            // 🔥 检查是否是服务器存储确认响应
            if (data.msgId && data.stored) {
                console.log('✅ 收到服务器存储确认:', data.msgId, 'serverMsgId:', data.serverMsgId);
                confirmMessageSent(data.msgId);
                return;
            }
            
            // 🔥 检查是否是接收者确认响应
            if (data.msgId && data.ack) {
                console.log('✅ 收到接收者确认:', data.msgId);
                // 不需要做任何操作，因为存储确认时已经标记为已送达
                return;
            }
            
            // 判断是否是当前聊天窗口
            let isCurrentChat = false;
            if (currentChat) {
                console.log('检查消息匹配:');
                console.log('  - conversationType:', data.conversationType);
                console.log('  - receiverId:', data.receiverId);
                console.log('  - senderId:', data.senderId);
                console.log('  - currentChat.targetId:', currentChat.targetId);
                console.log('  - currentChat.type:', currentChat.type);
                
                if (data.conversationType === 'group') {
                    // 群聊：检查receiverId(群ID)是否匹配
                    isCurrentChat = data.receiverId === currentChat.targetId;
                    console.log('  - 群聊匹配结果:', isCurrentChat);
                } else {
                    // 单聊：检查senderId是否匹配
                    isCurrentChat = data.senderId === currentChat.targetId;
                    console.log('  - 单聊匹配结果:', isCurrentChat);
                }
            } else {
                console.log('  - 当前没有打开聊天窗口');
            }
            
            // 🔥 如果是自己发的消息，跳过显示（已在sendMessage中显示）
            const msgSenderId = typeof data.senderId === 'string' ? parseInt(data.senderId) : data.senderId;
            const currentUid = typeof currentUser?.userId === 'string' ? parseInt(currentUser.userId) : currentUser?.userId;
            if (msgSenderId === currentUid) {
                console.log('⏭️ 跳过显示自己发送的消息（已在发送时显示）');
                return;
            }

            if (isCurrentChat) {
                console.log('✅ 添加消息到UI');
                addMessageToUI({
                    senderId: data.senderId,
                    content: data.content,
                    msgType: data.msgType || 'text',
                    extra: data.extra,
                    fileSize: data.fileSize,
                    timestamp: data.timestamp,
                    sender: 'other',
                    conversationType: data.conversationType,
                    msgId: data.msgId // 🔥 传递消息ID
                });
                
                // 🔥 关键修复：将接收到的消息保存到本地缓存，防止刷新后丢失
                saveReceivedMessageToLocal(data);
                
                // 🔥 接收者自动发送确认（如果是别人发的消息）
                const msgSenderId = typeof data.senderId === 'string' ? parseInt(data.senderId) : data.senderId;
                const currentUid = typeof currentUser?.userId === 'string' ? parseInt(currentUser.userId) : currentUser?.userId;
                if (msgSenderId !== currentUid && data.msgId) {
                    console.log('📤 接收者发送确认给发送者:', data.msgId, 'senderId:', data.senderId);
                    sendAck(data.msgId, data.senderId);
                }
                // 正在该会话窗口内收消息：立即清服务端未读，避免角标滞后（他人发消息时仍累加未读）
                void clearUnreadForContact(currentChat.targetId, currentChat.type);
            } else {
                console.log('❌ 不是当前聊天窗口，显示通知');
                
                // 🔥 关键修复：即使不在当前聊天窗口，也保存消息到本地缓存
                saveReceivedMessageToLocal(data);
                
                // 🔥 刷新会话列表以更新未读数和最新消息
                loadConversations().then(() => {
                    // 刷新后重新渲染好友列表以显示未读徽章
                    renderFriendsList();
                });
                
                showBrowserNotification(data);
            }
            break;
        case 'notification':
            showBrowserNotification(data);
            // 如果是好友申请通知，刷新列表
            if (data.content && data.content.includes('好友申请')) {
                loadFriendRequests();
            }
            break;
        case 'disconnected':
            console.warn('WebSocket已断开');
            break;
        case 'reconnect_failed':
            alert('连接失败，请刷新页面重试');
            break;
    }
}

// 显示浏览器通知
function showBrowserNotification(data) {
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('新消息', {
            body: data.content
        });
    }
}

// ========================================
// UI交互功能
// ========================================

// 显示添加好友弹窗
function showAddFriendModal() {
    const modal = document.getElementById('add-friend-modal');
    const input = document.getElementById('add-friend-id');
    
    if (modal) {
        modal.style.display = 'flex';
    }
    
    if (input) {
        input.value = '';
        input.focus();
    }
}

// 关闭添加好友弹窗
function closeAddFriendModal() {
    document.getElementById('add-friend-modal').style.display = 'none';
}

// 搜索用户ID并显示资料
async function searchUserById() {
    const userId = parseInt(document.getElementById('add-friend-id').value);
    
    if (!userId) {
        alert('请输入用户ID');
        return;
    }
    
    if (userId === currentUser.userId) {
        alert('不能查看自己的资料');
        return;
    }
    
    try {
        // 获取用户信息
        const response = await api.getUserInfo(userId);
        if (response.code === 200 && response.data) {
            const user = response.data;
            
            // 检查是否已经是好友
            const isFriend = friends.some(f => f.userId === userId);
            
            // 填充资料界面
            document.getElementById('friend-profile-display-name').textContent = user.nickname || user.username;
            document.getElementById('friend-profile-display-id').textContent = user.userId;
            document.getElementById('friend-profile-nickname').value = user.nickname || '';
            document.getElementById('friend-profile-username').value = user.username || '';
            document.getElementById('friend-profile-remark').value = '';
            document.getElementById('friend-add-time').value = '尚未添加';
            
            // 更新状态
            const statusBadge = document.getElementById('friend-status-badge');
            if (user.status === 'online') {
                statusBadge.textContent = '在线';
                statusBadge.classList.add('online');
            } else {
                statusBadge.textContent = '离线';
                statusBadge.classList.remove('online');
            }
            
            // 更新头像
            const avatarImg = document.getElementById('friend-profile-avatar-img');
            const avatarText = document.getElementById('friend-profile-avatar-text');
            
            if (user.avatar) {
                const avatarUrl = user.avatar.startsWith('http') ? user.avatar : `${API_BASE_URL}${user.avatar}`;
                avatarImg.src = avatarUrl;
                avatarImg.style.display = 'block';
                avatarText.style.display = 'none';
            } else {
                avatarImg.style.display = 'none';
                avatarText.style.display = 'block';
                avatarText.textContent = (user.nickname || user.username || '?')[0];
            }
            
            // 保存当前查看的用户ID
            window.currentViewingFriendId = userId;
            
            // 根据是否是好友显示不同按钮
            const btnSendRequest = document.getElementById('btn-send-request');
            if (isFriend) {
                btnSendRequest.textContent = '✅ 已是好友';
                btnSendRequest.disabled = true;
                btnSendRequest.style.opacity = '0.5';
            } else {
                btnSendRequest.textContent = '📨 发送好友申请';
                btnSendRequest.disabled = false;
                btnSendRequest.style.opacity = '1';
            }
            
            // 关闭添加好友弹窗，显示资料弹窗
            closeAddFriendModal();
            document.getElementById('friend-profile-modal').style.display = 'flex';
        } else {
            alert(response.message || '用户不存在');
        }
    } catch (error) {
        console.error('搜索用户错误:', error);
        alert('搜索失败，请检查网络连接');
    }
}

// 从资料界面发送好友申请
async function sendFriendRequestFromProfile() {
    const targetUserId = window.currentViewingFriendId;
    if (!targetUserId) {
        alert('用户信息错误');
        return;
    }
    
    console.log('📨 开始发送好友申请:', {
        fromUserId: currentUser.userId,
        toUserId: targetUserId,
        fromNickname: currentUser.nickname
    });
    
    // 创建或获取与该用户的会话
    const convId = Math.min(currentUser.userId, targetUserId) * 100000 + Math.max(currentUser.userId, targetUserId);
    
    // 构造好友申请消息
    const requestMessage = {
        type: 'friend_request',
        content: `${currentUser.nickname || currentUser.username}请求添加你为好友`,
        senderId: currentUser.userId,
        targetId: targetUserId,
        convId: convId,
        extra: JSON.stringify({
            type: 'friend_request',
            fromUserId: currentUser.userId,
            fromUsername: currentUser.username,
            fromNickname: currentUser.nickname,
            fromAvatar: currentUser.avatar,
            targetId: targetUserId
        })
    };
    
    console.log('📨 好友申请消息内容:', requestMessage);
    
    try {
        // 通过WebSocket发送消息
        const sent = wsManager.send(requestMessage);
        console.log('📨 WebSocket发送结果:', sent ? '成功' : '失败');
        
        if (sent) {
            alert('好友申请已发送！');
            closeFriendProfileModal();
        } else {
            alert('发送失败，WebSocket未连接');
        }
    } catch (error) {
        console.error('❌ 发送申请错误:', error);
        alert('发送失败，请检查网络连接');
    }
}

// 显示创建群聊弹窗
function showCreateGroupModal() {
    const modal = document.getElementById('create-group-modal');
    const memberSelect = document.getElementById('group-member-select');
    
    // 清空表单
    document.getElementById('create-group-name').value = '';
    
    // 加载好友列表供选择
    if (friends.length === 0) {
        memberSelect.innerHTML = '<div style="text-align: center; color: #999; padding: 20px;">暂无好友，请先添加好友</div>';
    } else {
        memberSelect.innerHTML = friends.map(friend => {
            const displayName = friend.remark || friend.nickname || friend.username;
            return `
                <label style="display: flex; align-items: center; padding: 8px; cursor: pointer; border-radius: 6px; transition: background 0.2s;" onmouseover="this.style.background='#f5f5f5'" onmouseout="this.style.background='transparent'">
                    <input type="checkbox" value="${friend.userId}" style="margin-right: 10px; width: 18px; height: 18px; cursor: pointer;">
                    <span>${escapeHtml(displayName)}</span>
                </label>
            `;
        }).join('');
    }
    
    modal.style.display = 'flex';
}

// 关闭创建群聊弹窗
function closeCreateGroupModal() {
    document.getElementById('create-group-modal').style.display = 'none';
}

// 处理创建群聊
async function handleCreateGroup() {
    const groupName = document.getElementById('create-group-name').value.trim();
    
    if (!groupName) {
        alert('请输入群名称');
        return;
    }
    
    // 获取选中的成员
    const checkboxes = document.querySelectorAll('#group-member-select input[type="checkbox"]:checked');
    const memberIds = Array.from(checkboxes).map(cb => parseInt(cb.value));
    
    try {
        const response = await api.createGroup(currentUser.userId, groupName, null, memberIds);
        
        if (response.code === 200) {
            alert('群聊创建成功！');
            closeCreateGroupModal();
            await loadGroups();
            
            // 切换到群组标签
            const groupTab = document.querySelector('.tab-btn:nth-child(2)');
            if (groupTab) groupTab.click();
        } else {
            alert(response.message || '创建群聊失败');
        }
    } catch (error) {
        console.error('创建群聊错误:', error);
        alert('创建群聊失败，请检查网络连接');
    }
}

// 显示群聊编辑弹窗
async function showEditGroupModal() {
    if (!currentChat || currentChat.type !== 'group') {
        alert('当前不是群聊');
        return;
    }
    
    const groupId = currentChat.targetId;
    
    try {
        const response = await api.getGroupDetail(groupId);
        if (response.code === 200 && response.data) {
            const { group, members, memberCount } = response.data;
            
            // 填充表单
            document.getElementById('edit-group-name').value = group.groupName || '';
            document.getElementById('edit-group-announcement').value = group.announcement || '';
            
            // 显示头像
            const avatarPreview = document.getElementById('edit-group-avatar-preview');
            if (group.avatar) {
                // 处理头像URL，确保使用完整路径
                const avatarUrl = group.avatar.startsWith('http') ? group.avatar : `${API_BASE_URL}${group.avatar}`;
                avatarPreview.style.backgroundImage = `url('${avatarUrl}')`;
                avatarPreview.style.backgroundSize = 'cover';
                avatarPreview.style.backgroundPosition = 'center';
                avatarPreview.textContent = '';
            } else {
                avatarPreview.style.backgroundImage = '';
                avatarPreview.textContent = '👥';
            }
            
            // 保存当前群ID和头像URL
            window.currentEditingGroupId = groupId;
            window.currentGroupAvatar = group.avatar;
            
            // 显示成员列表
            const membersList = document.getElementById('edit-group-members-list');
            if (members && members.length > 0) {
                membersList.innerHTML = members.map(member => {
                    const roleText = member.role === 'owner' ? '群主' : (member.role === 'admin' ? '管理员' : '成员');
                    return `<div style="padding: 8px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center;">
                        <span>用户ID: ${member.userId}</span>
                        <span style="color: #999; font-size: 12px;">${roleText}</span>
                    </div>`;
                }).join('');
            } else {
                membersList.innerHTML = '<div style="text-align: center; color: #999; padding: 20px;">暂无成员</div>';
            }
            
            document.getElementById('edit-group-modal').style.display = 'flex';
        } else {
            alert(response.message || '获取群详情失败');
        }
    } catch (error) {
        console.error('获取群详情错误:', error);
        alert('获取群详情失败，请检查网络连接');
    }
}

// 关闭群聊编辑弹窗
function closeEditGroupModal() {
    document.getElementById('edit-group-modal').style.display = 'none';
}

// 触发群头像上传
function triggerGroupAvatarUpload() {
    document.getElementById('group-avatar-upload').click();
}

// 处理群头像上传
async function handleGroupAvatarUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (!file.type.startsWith('image/')) {
        alert('请选择图片文件');
        return;
    }
    
    if (file.size > 5 * 1024 * 1024) {
        alert('图片大小不能超过5MB');
        return;
    }
    
    try {
        const response = await api.uploadFile(file);
        if (response.code === 200 && response.data) {
            const avatarUrl = response.data.url;
            
            // 更新预览
            const avatarPreview = document.getElementById('edit-group-avatar-preview');
            avatarPreview.style.backgroundImage = `url('${avatarUrl}')`;
            avatarPreview.style.backgroundSize = 'cover';
            avatarPreview.style.backgroundPosition = 'center';
            avatarPreview.textContent = '';
            
            // 临时保存
            window.pendingGroupAvatar = avatarUrl;
            
            alert('头像上传成功，请点击保存按钮保存更改');
        } else {
            alert(response.message || '头像上传失败');
        }
    } catch (error) {
        console.error('头像上传错误:', error);
        alert('头像上传失败，请检查网络连接');
    }
}

// 保存群设置
async function saveGroupSettings() {
    const groupId = window.currentEditingGroupId;
    if (!groupId) {
        alert('群ID不存在');
        return;
    }
    
    const groupName = document.getElementById('edit-group-name').value.trim();
    const announcement = document.getElementById('edit-group-announcement').value.trim();
    const avatar = window.pendingGroupAvatar || window.currentGroupAvatar;
    
    if (!groupName) {
        alert('请输入群名称');
        return;
    }
    
    try {
        const response = await api.updateGroup(groupId, groupName, avatar, announcement);
        
        if (response.code === 200) {
            alert('群设置保存成功！');
            closeEditGroupModal();
            
            // 更新当前聊天名称
            if (currentChat && currentChat.targetId === groupId) {
                document.getElementById('chat-name').textContent = groupName;
            }
            
            // 重新加载群列表
            await loadGroups();
            
            // 清除临时数据
            window.pendingGroupAvatar = null;
        } else {
            alert(response.message || '保存失败');
        }
    } catch (error) {
        console.error('保存群设置错误:', error);
        alert('保存失败，请检查网络连接');
    }
}

// 显示群头像大图
function showGroupAvatarModal() {
    if (!currentChat || currentChat.type !== 'group') {
        return; // 只有群聊才显示
    }
    
    const groupId = currentChat.targetId;
    const group = groups.find(g => g.groupId === groupId);
    
    if (group && group.avatar) {
        // 处理头像URL，确保使用完整路径
        const avatarUrl = group.avatar.startsWith('http') ? group.avatar : `${API_BASE_URL}${group.avatar}`;
        document.getElementById('group-avatar-large').src = avatarUrl;
        document.getElementById('group-avatar-modal').style.display = 'flex';
    }
}

// 关闭群头像弹窗
function closeGroupAvatarModal() {
    document.getElementById('group-avatar-modal').style.display = 'none';
}

// 处理添加好友
async function handleAddFriend() {
    const friendId = parseInt(document.getElementById('add-friend-id').value);
    const remark = document.getElementById('add-friend-remark').value.trim();

    if (!friendId) {
        alert('请输入好友用户ID');
        return;
    }

    if (friendId === currentUser.userId) {
        alert('不能添加自己为好友');
        return;
    }

    try {
        const response = await api.addFriend(friendId, remark || null);

        if (response.code === 200) {
            alert('添加好友成功！');
            closeAddFriendModal();
            await loadFriends();
        } else {
            alert(response.message || '添加好友失败');
        }
    } catch (error) {
        console.error('添加好友错误:', error);
        alert('添加好友失败，请检查网络连接');
    }
}

// 切换标签
function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');

    document.querySelectorAll('.contact-list').forEach(list => {
        list.classList.remove('active');
    });
    document.getElementById(`${tab}-list`).classList.add('active');
    
    // 如果切换到好友申请标签，加载申请列表并启动定时刷新
    if (tab === 'requests') {
        loadFriendRequests();
        startFriendRequestRefresh();
    } else {
        stopFriendRequestRefresh();
    }
}

// 搜索功能
function handleSearch() {
    const keyword = document.getElementById('search-input').value.toLowerCase();

    const friendItems = document.querySelectorAll('#friends-list .contact-item');
    friendItems.forEach(item => {
        const name = item.querySelector('.contact-name').textContent.toLowerCase();
        item.style.display = name.includes(keyword) ? 'flex' : 'none';
    });

    const groupItems = document.querySelectorAll('#groups-list .contact-item');
    groupItems.forEach(item => {
        const nameEl = item.querySelector('.contact-name');
        if (nameEl) {
            const name = nameEl.textContent.toLowerCase();
            item.style.display = name.includes(keyword) ? 'flex' : 'none';
        }
    });
}

// 切换侧边栏(移动端)
function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('open');
}

// 请求通知权限
if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission();
}

// 显示好友右键菜单
function showFriendContextMenu(friendId, displayName, nickname, remark) {
    const menuHtml = `
        <div class="context-menu" id="friend-context-menu" style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: white; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); z-index: 1000; min-width: 180px;">
            <div class="menu-item" onclick="viewFriendInfo(${friendId})">👤 查看资料</div>
            <div class="menu-item" onclick="editFriendRemark(${friendId}, '${escapeHtml(displayName)}', '${escapeHtml(remark)}')">✏️ 修改备注</div>
            <div class="menu-item" onclick="clearFriendChatHistory(${friendId})">🗑️ 清空聊天记录</div>
            <div class="menu-item" onclick="deleteFriend(${friendId}, '${escapeHtml(displayName)}')" style="color: #ff4444;">🗑️ 删除好友</div>
            <div class="menu-item" onclick="closeContextMenu()">❌ 关闭</div>
        </div>
        <div class="context-menu-overlay" onclick="closeContextMenu()" style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 999;"></div>
    `;
    
    // 移除已存在的菜单
    const existingMenu = document.getElementById('friend-context-menu');
    if (existingMenu) existingMenu.remove();
    const existingOverlay = document.querySelector('.context-menu-overlay');
    if (existingOverlay) existingOverlay.remove();
    
    document.body.insertAdjacentHTML('beforeend', menuHtml);
}

// 关闭右键菜单
function closeContextMenu() {
    const menu = document.getElementById('friend-context-menu');
    if (menu) menu.remove();
    const overlay = document.querySelector('.context-menu-overlay');
    if (overlay) overlay.remove();
}

// 清空好友聊天记录
function clearFriendChatHistory(friendId) {
    closeContextMenu();
    
    const friend = friends.find(f => f.userId === friendId);
    const displayName = friend ? (friend.remark || friend.nickname || friend.username) : '该好友';
    
    if (!confirm(`确定要清空与「${displayName}」的聊天记录吗？\n注意：此操作仅清除当前设备的显示，对方仍可查看。`)) {
        return;
    }
    
    // 计算 convId
    const convId = Math.min(currentUser.userId, friendId) * 100000 + Math.max(currentUser.userId, friendId);
    
    // 清空本地缓存
    try {
        const cacheKey = `chat_history_${convId}_${currentUser.userId}`;
        localStorage.removeItem(cacheKey);
        console.log('🗑️ 已清空本地缓存:', cacheKey);
    } catch (error) {
        console.error('清空缓存失败:', error);
    }
    
    // 如果当前正在与该好友聊天，清空 UI
    if (currentChat && currentChat.type === 'friend' && currentChat.targetId === friendId) {
        const messageList = document.getElementById('message-list');
        messageList.innerHTML = '';
        addSystemMessage(`这是与 ${displayName} 的开始`);
    }
    
    showNotification('聊天记录已清空', 'success');
}

// 编辑好友备注
async function editFriendRemark(friendId, displayName, currentRemark) {
    closeContextMenu();
    
    const newRemark = prompt('请输入备注名称：', currentRemark || '');
    if (newRemark === null) return; // 用户取消
    
    try {
        const response = await api.setRemark(friendId, newRemark || null);
        if (response.code === 200) {
            alert('备注修改成功！');
            await loadFriends();
            
            // 如果当前正在与该好友聊天，更新聊天头部显示
            if (currentChat && currentChat.type === 'friend' && currentChat.targetId === friendId) {
                const friend = friends.find(f => f.userId === friendId);
                const chatName = friend ? (friend.remark || friend.nickname || friend.username) : displayName;
                document.getElementById('chat-name').textContent = chatName;
            }
        } else {
            alert(response.message || '修改失败');
        }
    } catch (error) {
        console.error('修改备注错误:', error);
        alert('修改备注失败，请检查网络连接');
    }
}

// 查看好友资料
async function viewFriendInfo(friendId) {
    closeContextMenu();
    
    try {
        const response = await api.getFriendInfo(currentUser.userId, friendId);
        if (response.code === 200 && response.data) {
            const info = response.data;
            const friend = friends.find(f => f.userId === friendId);
            const displayName = friend ? (friend.remark || friend.nickname || friend.username) : info.nickname;
            
            // 安全检查DOM元素
            const displayNameEl = document.getElementById('friend-profile-display-name');
            const displayIdEl = document.getElementById('friend-profile-display-id');
            const nicknameEl = document.getElementById('friend-profile-nickname');
            const usernameEl = document.getElementById('friend-profile-username');
            const remarkEl = document.getElementById('friend-profile-remark');
            const addTimeEl = document.getElementById('friend-add-time');
            const statusBadge = document.getElementById('friend-status-badge');
            const avatarImg = document.getElementById('friend-profile-avatar-img');
            const avatarText = document.getElementById('friend-profile-avatar-text');
            const modal = document.getElementById('friend-profile-modal');
            
            if (!displayNameEl || !modal) {
                console.error('❌ 资料弹窗DOM元素不存在');
                alert('系统错误，请刷新页面');
                return;
            }
            
            // 填充好友资料
            if (displayNameEl) displayNameEl.textContent = displayName;
            if (displayIdEl) displayIdEl.textContent = info.userId;
            if (nicknameEl) nicknameEl.value = info.nickname || '';
            if (usernameEl) usernameEl.value = info.username || '';
            if (remarkEl) remarkEl.value = friend?.remark || '';
            if (addTimeEl) addTimeEl.value = friend?.createTime || '未知';
            
            // 更新状态
            if (statusBadge) {
                if (info.status === 'online') {
                    statusBadge.textContent = '在线';
                    statusBadge.classList.add('online');
                } else {
                    statusBadge.textContent = '离线';
                    statusBadge.classList.remove('online');
                }
            }
            
            // 更新头像
            if (avatarImg && avatarText) {
                if (info.avatar) {
                    const avatarUrl = info.avatar.startsWith('http') ? info.avatar : `${API_BASE_URL}${info.avatar}`;
                    avatarImg.src = avatarUrl;
                    avatarImg.style.display = 'block';
                    avatarText.style.display = 'none';
                } else {
                    avatarImg.style.display = 'none';
                    avatarText.style.display = 'block';
                    avatarText.textContent = (info.nickname || info.username || '?')[0];
                }
            }
            
            // 保存当前好友ID
            window.currentViewingFriendId = friendId;
            
            // 更新按钮状态（已是好友）
            const btnSendRequest = document.getElementById('btn-send-request');
            if (btnSendRequest) {
                btnSendRequest.textContent = '✅ 已是好友';
                btnSendRequest.disabled = true;
                btnSendRequest.style.opacity = '0.5';
            }
            
            // 显示弹窗
            modal.style.display = 'flex';
        } else {
            alert(response.message || '获取资料失败');
        }
    } catch (error) {
        console.error('获取好友资料错误:', error);
        alert('获取资料失败，请检查网络连接');
    }
}

// 关闭好友资料弹窗
function closeFriendProfileModal() {
    document.getElementById('friend-profile-modal').style.display = 'none';
}

// 保存好友备注
async function saveFriendRemark() {
    const friendId = window.currentViewingFriendId;
    if (!friendId) return;
    
    const remark = document.getElementById('friend-profile-remark').value.trim();
    
    try {
        const response = await api.setRemark(friendId, remark || null);
        if (response.code === 200) {
            // 更新本地数据
            const friend = friends.find(f => f.userId === friendId);
            if (friend) {
                friend.remark = remark || null;
            }
            
            // 刷新列表
            await loadFriends();
            
            // 更新聊天标题
            if (currentChat && currentChat.type === 'friend' && currentChat.targetId === friendId) {
                const displayName = remark || friend?.nickname || friend?.username || '未知';
                document.getElementById('chat-name').textContent = displayName;
            }
            
            alert('备注保存成功！');
        } else {
            alert(response.message || '保存失败');
        }
    } catch (error) {
        console.error('保存备注错误:', error);
        alert('保存失败，请检查网络连接');
    }
}

// 发消息给好友
function sendMessageToFriend() {
    const friendId = window.currentViewingFriendId;
    if (!friendId) return;
    
    closeFriendProfileModal();
    
    const friend = friends.find(f => f.userId === friendId);
    if (friend) {
        const displayName = friend.remark || friend.nickname || friend.username;
        selectChat('friend', friendId, displayName);
    }
}

// 删除好友
async function deleteFriend(friendId, displayName) {
    closeContextMenu();
    
    if (!confirm(`确定要删除好友“${displayName}”吗？`)) {
        return;
    }
    
    try {
        const response = await api.removeFriend(friendId);
        if (response.code === 200) {
            alert('删除好友成功！');
            await loadFriends();
        } else {
            alert(response.message || '删除失败');
        }
    } catch (error) {
        console.error('删除好友错误:', error);
        alert('删除好友失败，请检查网络连接');
    }
}

// ========================================
// 个人资料编辑功能
// ========================================

// 显示个人资料弹窗
function showProfileModal() {
    const modal = document.getElementById('profile-modal');
    if (!modal) return;
    
    // 填充当前用户信息
    document.getElementById('profile-nickname').value = currentUser.nickname || currentUser.username || '';
    document.getElementById('profile-username').value = currentUser.username || '';
    document.getElementById('profile-userid').value = currentUser.userId || '';
    
    // 更新头像预览
    const avatarImg = document.getElementById('profile-avatar-img');
    const avatarText = document.getElementById('profile-avatar-text');
    
    if (currentUser.avatar) {
        // 将相对路径转换为完整URL
        const avatarUrl = currentUser.avatar.startsWith('http') ? currentUser.avatar : `${API_BASE_URL}${currentUser.avatar}`;
        avatarImg.src = avatarUrl;
        avatarImg.style.display = 'block';
        avatarText.style.display = 'none';
    } else {
        avatarImg.style.display = 'none';
        avatarText.style.display = 'block';
        avatarText.textContent = (currentUser.nickname || currentUser.username || '?')[0];
    }
    
    modal.style.display = 'flex';
}

// 关闭个人资料弹窗
function closeProfileModal() {
    const modal = document.getElementById('profile-modal');
    if (modal) {
        modal.style.display = 'none';
    }
}

// 触发头像上传
function triggerAvatarUpload() {
    document.getElementById('avatar-upload').click();
}

// 处理头像上传
async function handleAvatarUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    // 检查文件类型
    if (!file.type.startsWith('image/')) {
        alert('请选择图片文件');
        return;
    }
    
    // 检查文件大小（5MB）
    if (file.size > 5 * 1024 * 1024) {
        alert('图片大小不能超过5MB');
        return;
    }
    
    try {
        // 上传头像
        const response = await api.uploadFile(file);
        if (response.code === 200 && response.data) {
            const avatarUrl = response.data.url;
            
            // 更新预览
            const avatarImg = document.getElementById('profile-avatar-img');
            const avatarText = document.getElementById('profile-avatar-text');
            avatarImg.src = avatarUrl;
            avatarImg.style.display = 'block';
            avatarText.style.display = 'none';
            
            // 临时保存头像URL
            currentUser.pendingAvatar = avatarUrl;
            
            alert('头像上传成功，请点击保存按钮保存更改');
        } else {
            alert(response.message || '头像上传失败');
        }
    } catch (error) {
        console.error('头像上传错误:', error);
        alert('头像上传失败，请检查网络连接');
    }
}

// 保存个人资料
async function saveProfile() {
    const nickname = document.getElementById('profile-nickname').value.trim();
    
    if (!nickname) {
        showNotification('昵称不能为空', 'error');
        return;
    }
    
    try {
        // 更新用户信息
        const updateData = {
            nickname: nickname
        };
        
        // 如果有新头像，也一起更新
        if (currentUser.pendingAvatar) {
            updateData.avatar = currentUser.pendingAvatar;
            delete currentUser.pendingAvatar;
        }
        
        const response = await api.updateUserInfo(currentUser.userId, updateData);
        
        if (response.code === 200) {
            // 更新本地用户信息
            currentUser.nickname = nickname;
            if (updateData.avatar) {
                currentUser.avatar = updateData.avatar;
            }
            localStorage.setItem('user', JSON.stringify(currentUser));
            
            // 更新界面显示
            updateUserAvatar();
            document.getElementById('current-username').textContent = nickname;
            
            closeProfileModal();
            showNotification('保存成功！', 'success');
        } else {
            showNotification(response.message || '保存失败', 'error');
        }
    } catch (error) {
        console.error('保存资料错误:', error);
        showNotification('保存失败，请检查网络连接', 'error');
    }
}

// 通知提示函数
function showNotification(message, type = 'info') {
    const icons = {
        success: '✅',
        error: '❌',
        info: 'ℹ️'
    };
    
    // 创建一个临时通知
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        left: 50%;
        transform: translateX(-50%);
        background: ${type === 'error' ? '#FF4D4F' : type === 'success' ? '#52C41A' : '#5B8DEF'};
        color: white;
        padding: 12px 24px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000;
        font-size: 14px;
        animation: slideDown 0.3s ease-out;
    `;
    notification.textContent = `${icons[type]} ${message}`;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'fadeOut 0.3s ease-out';
        setTimeout(() => notification.remove(), 300);
    }, 2000);
}

// ========================================
// 🔥 消息确认机制相关函数
// ========================================

// 生成唯一消息ID
function generateMessageId() {
    return 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

// 保存待确认消息到浏览器缓存
function savePendingMessage(msgId, messageData) {
    try {
        // 🔥 为每个用户独立缓存，使用 userId 作为 key
        const cacheKey = `chat_messages_${currentUser.userId}`;
        const cached = JSON.parse(localStorage.getItem(cacheKey) || '{}');
        cached[msgId] = messageData;
        localStorage.setItem(cacheKey, JSON.stringify(cached));
        pendingMessages.set(msgId, messageData);
        console.log('💾 消息已保存到缓存:', msgId, '用户:', currentUser.userId);
    } catch (error) {
        console.error('保存消息到缓存失败:', error);
    }
}

// 更新消息状态
function updateMessageStatus(msgId, status) {
    try {
        // 🔥 使用用户独立的缓存key
        const cacheKey = `chat_messages_${currentUser.userId}`;
        const cached = JSON.parse(localStorage.getItem(cacheKey) || '{}');
        if (cached[msgId]) {
            cached[msgId].status = status;
            localStorage.setItem(cacheKey, JSON.stringify(cached));
            
            // 更新内存中的状态
            if (pendingMessages.has(msgId)) {
                pendingMessages.get(msgId).status = status;
            }
            
            // 更新UI显示
            updateMessageUIStatus(msgId, status);
            console.log('✅ 消息状态更新为:', status, msgId);
        }
    } catch (error) {
        console.error('更新消息状态失败:', error);
    }
}

// 更新UI中消息的状态显示
function updateMessageUIStatus(msgId, status) {
    const messageList = document.getElementById('message-list');
    const messages = messageList.querySelectorAll('.message');
    
    console.log('🔍 更新消息状态UI:', {msgId, status, '消息总数': messages.length});
    
    messages.forEach(msgEl => {
        const bubble = msgEl.querySelector('.message-bubble');
        if (!bubble) return;
        
        const dataMsgId = bubble.getAttribute('data-msg-id');
        console.log('  检查消息元素:', {
            'data-msg-id': dataMsgId,
            '目标msgId': msgId,
            '匹配': dataMsgId === msgId
        });
        
        if (dataMsgId === msgId) {
            // 移除旧的状态标记
            const oldStatus = bubble.querySelector('.msg-status');
            if (oldStatus) {
                oldStatus.remove();
                console.log('  ✅ 移除旧状态标记');
            }
            
            // 🔥 只在发送中时显示图标，成功后直接移除，不显示对号
            if (status === 'sending') {
                const statusIcon = '<span class="msg-status">⏳</span>';
                bubble.insertAdjacentHTML('beforeend', statusIcon);
                console.log('  ✅ 添加发送中状态标记');
            } else if (status === 'failed') {
                const statusIcon = '<span class="msg-status" style="color: #ff4444;">✗</span>';
                bubble.insertAdjacentHTML('beforeend', statusIcon);
                console.log('  ✅ 添加失败状态标记');
            } else if (status === 'sent') {
                // 🔥 成功时不显示任何图标，保持干净
                console.log('  ✅ 消息发送成功，不显示状态图标');
            }
        }
    });
}

// 设置消息超时（10秒）
function setupMessageTimeout(msgId) {
    const timeout = setTimeout(() => {
        if (pendingMessages.has(msgId)) {
            const msg = pendingMessages.get(msgId);
            if (msg.status === 'sending') {
                console.warn('⚠️ 消息发送超时:', msgId);
                updateMessageStatus(msgId, 'failed');
                pendingMessages.delete(msgId);
                
                // 显示通知
                showNotification('消息发送超时，请重试', 'error');
            }
        }
    }, 10000); // 10秒超时
    
    // 保存timeout ID以便取消
    if (pendingMessages.has(msgId)) {
        pendingMessages.get(msgId).timeout = timeout;
    }
}

// 确认消息发送成功
function confirmMessageSent(msgId) {
    console.log('🎉 确认消息发送成功:', msgId);
    console.log('  pendingMessages 中是否存在:', pendingMessages.has(msgId));
    
    // 🔥 关键修复：先更新UI状态，再处理pendingMessages
    updateMessageStatus(msgId, 'sent');
    
    if (pendingMessages.has(msgId)) {
        const msg = pendingMessages.get(msgId);
        console.log('  消息状态:', msg.status);
        // 清除超时定时器
        if (msg.timeout) {
            clearTimeout(msg.timeout);
            console.log('  ✅ 清除超时定时器');
        }
        pendingMessages.delete(msgId);
        console.log('  ✅ 消息已从pendingMessages移除');
    } else {
        console.warn('  ⚠️ 消息不在 pendingMessages 中（可能已超时或已处理）');
        // 🔥 即使不在pendingMessages中，也要确保localStorage中的状态被更新
        try {
            const cacheKey = `chat_messages_${currentUser.userId}`;
            const cached = JSON.parse(localStorage.getItem(cacheKey) || '{}');
            if (cached[msgId]) {
                cached[msgId].status = 'sent';
                localStorage.setItem(cacheKey, JSON.stringify(cached));
                console.log('  ✅ 已更新localStorage中的消息状态');
            }
        } catch (error) {
            console.error('更新localStorage失败:', error);
        }
    }
}

// 发送消息确认（接收者调用）
function sendAck(msgId, senderId) {
    const ackMessage = {
        type: 'ack',
        msgId: msgId,
        receiverId: currentUser.userId
    };
    
    if (wsManager.send(ackMessage)) {
        console.log('📤 已发送消息确认:', msgId, '给:', senderId);
    } else {
        console.warn('⚠️ 发送消息确认失败');
    }
}
// ========================================
// 个人资料编辑功能
// ========================================

// 显示个人资料弹窗
function showProfileModal() {
    const modal = document.getElementById('profile-modal');
    if (!modal) return;
    
    // 填充当前用户信息
    document.getElementById('profile-nickname').value = currentUser.nickname || currentUser.username || '';
    document.getElementById('profile-username').value = currentUser.username || '';
    document.getElementById('profile-userid').value = currentUser.userId || '';
    
    // 更新头像预览
    const avatarImg = document.getElementById('profile-avatar-img');
    const avatarText = document.getElementById('profile-avatar-text');
    
    if (currentUser.avatar) {
        // 将相对路径转换为完整URL
        const avatarUrl = currentUser.avatar.startsWith('http') ? currentUser.avatar : `${API_BASE_URL}${currentUser.avatar}`;
        avatarImg.src = avatarUrl;
        avatarImg.style.display = 'block';
        avatarText.style.display = 'none';
    } else {
        avatarImg.style.display = 'none';
        avatarText.style.display = 'block';
        avatarText.textContent = (currentUser.nickname || currentUser.username || '?')[0];
    }
    
    modal.style.display = 'flex';
}

// 关闭个人资料弹窗
function closeProfileModal() {
    const modal = document.getElementById('profile-modal');
    if (modal) {
        modal.style.display = 'none';
    }
}

// 触发头像上传
function triggerAvatarUpload() {
    document.getElementById('avatar-upload').click();
}

// 处理头像上传
async function handleAvatarUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    // 检查文件类型
    if (!file.type.startsWith('image/')) {
        alert('请选择图片文件');
        return;
    }
    
    // 检查文件大小（5MB）
    if (file.size > 5 * 1024 * 1024) {
        alert('图片大小不能超过5MB');
        return;
    }
    
    try {
        // 上传头像
        const response = await api.uploadFile(file);
        if (response.code === 200 && response.data) {
            const avatarUrl = response.data.url;
            
            // 更新预览
            const avatarImg = document.getElementById('profile-avatar-img');
            const avatarText = document.getElementById('profile-avatar-text');
            avatarImg.src = avatarUrl;
            avatarImg.style.display = 'block';
            avatarText.style.display = 'none';
            
            // 临时保存头像URL
            currentUser.pendingAvatar = avatarUrl;
            
            alert('头像上传成功，请点击保存按钮保存更改');
        } else {
            alert(response.message || '头像上传失败');
        }
    } catch (error) {
        console.error('头像上传错误:', error);
        alert('头像上传失败，请检查网络连接');
    }
}

// 保存个人资料
async function saveProfile() {
    const nickname = document.getElementById('profile-nickname').value.trim();
    
    if (!nickname) {
        showNotification('昵称不能为空', 'error');
        return;
    }
    
    try {
        // 更新用户信息
        const updateData = {
            nickname: nickname
        };
        
        // 如果有新头像，也一起更新
        if (currentUser.pendingAvatar) {
            updateData.avatar = currentUser.pendingAvatar;
            delete currentUser.pendingAvatar;
        }
        
        const response = await api.updateUserInfo(currentUser.userId, updateData);
        
        if (response.code === 200) {
            // 更新本地用户信息
            currentUser.nickname = nickname;
            if (updateData.avatar) {
                currentUser.avatar = updateData.avatar;
            }
            localStorage.setItem('user', JSON.stringify(currentUser));
            
            // 更新界面显示
            updateUserAvatar();
            document.getElementById('current-username').textContent = nickname;
            
            closeProfileModal();
            showNotification('保存成功！', 'success');
        } else {
            showNotification(response.message || '保存失败', 'error');
        }
    } catch (error) {
        console.error('保存资料错误:', error);
        showNotification('保存失败，请检查网络连接', 'error');
    }
}

// 通知提示函数
function showNotification(message, type = 'info') {
    const icons = {
        success: '✅',
        error: '❌',
        info: 'ℹ️'
    };
    
    // 创建一个临时通知
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        left: 50%;
        transform: translateX(-50%);
        background: ${type === 'error' ? '#FF4D4F' : type === 'success' ? '#52C41A' : '#5B8DEF'};
        color: white;
        padding: 12px 24px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000;
        font-size: 14px;
        animation: slideDown 0.3s ease-out;
    `;
    notification.textContent = `${icons[type]} ${message}`;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'fadeOut 0.3s ease-out';
        setTimeout(() => notification.remove(), 300);
    }, 2000);
}
// ========================================
// 好友申请功能
// ========================================

let friendRequests = []; // 存储收到的好友申请

// 加载好友申请列表
async function loadFriendRequests() {
    try {
        const response = await api.getFriendRequests();
        if (response.code === 200 && response.data) {
            friendRequests = response.data;
            localStorage.setItem('friendRequests', JSON.stringify(friendRequests));
        }
    } catch (error) {
        console.error('加载好友申请失败:', error);
        // 降级：从localStorage加载
        const saved = localStorage.getItem('friendRequests');
        if (saved) {
            try {
                friendRequests = JSON.parse(saved);
            } catch (e) {
                friendRequests = [];
            }
        }
    }
    
    renderFriendRequests();
}

// 渲染好友申请列表
function renderFriendRequests() {
    const container = document.getElementById('requests-list');
    const noTip = document.getElementById('no-requests-tip');

    // 安全检查：确保DOM元素存在
    if (!container) {
        console.warn('⚠️ requests-list 容器不存在');
        return;
    }

    if (!friendRequests || friendRequests.length === 0) {
        container.innerHTML = '';
        if (noTip) {
            container.appendChild(noTip);
            noTip.style.display = 'block';
        }
        return;
    }

    if (noTip) {
        noTip.style.display = 'none';
    }

    container.innerHTML = friendRequests.map(request => {
        const avatarUrl = request.fromAvatar ? (request.fromAvatar.startsWith('http') ? request.fromAvatar : `${API_BASE_URL}${request.fromAvatar}`) : '';

        return `
            <div class="contact-item" style="padding: 15px;">
                <div class="avatar" style="${avatarUrl ? `background-image: url('${avatarUrl}'); background-size: cover;` : 'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);'}">
                    ${!avatarUrl ? (request.fromNickname || request.fromUsername || '?')[0] : ''}
                </div>
                <div class="contact-info" style="flex: 1;">
                    <div class="contact-name">${escapeHtml(request.fromNickname || request.fromUsername)}</div>
                    <div style="font-size: 12px; color: #999; margin-top: 4px;">请求添加你为好友</div>
                </div>
                <div class="contact-actions" style="display: flex; gap: 8px;">
                    <button onclick="acceptFriendRequest(${request.fromUserId})" class="btn-primary" style="padding: 6px 12px; font-size: 12px;">同意</button>
                    <button onclick="rejectFriendRequest(${request.fromUserId})" class="btn-cancel" style="padding: 6px 12px; font-size: 12px;">拒绝</button>
                </div>
            </div>
        `;
    }).join('');
}

// 接受好友申请
async function acceptFriendRequest(fromUserId) {
    try {
        const response = await api.addFriend(fromUserId, null);

        if (response.code === 200) {
            alert('已同意好友申请！');
            friendRequests = friendRequests.filter(r => r.fromUserId !== fromUserId);
            localStorage.setItem('friendRequests', JSON.stringify(friendRequests));
            renderFriendRequests();
            await loadFriends();
        } else {
            alert(response.message || '操作失败');
        }
    } catch (error) {
        console.error('接受申请错误:', error);
        alert('操作失败，请检查网络连接');
    }
}

// 拒绝好友申请
async function rejectFriendRequest(fromUserId) {
    if (confirm('确定要拒绝该好友申请吗？')) {
        try {
            const response = await api.rejectFriendRequest(currentUser.userId, fromUserId);
            if (response.code === 200) {
                friendRequests = friendRequests.filter(r => r.fromUserId !== fromUserId);
                localStorage.setItem('friendRequests', JSON.stringify(friendRequests));
                renderFriendRequests();
                showNotification('已拒绝好友申请', 'info');
            } else {
                alert(response.message || '操作失败');
            }
        } catch (error) {
            console.error('拒绝申请错误:', error);
            alert('操作失败，请检查网络连接');
        }
    }
}

// 启动定时刷新好友申请（每30秒）
let friendRequestRefreshTimer = null;
function startFriendRequestRefresh() {
    if (friendRequestRefreshTimer) return;
    
    friendRequestRefreshTimer = setInterval(() => {
        // 只在好友申请标签页时刷新
        const requestsTab = document.querySelector('.tab-btn.active');
        if (requestsTab && requestsTab.textContent.includes('好友申请')) {
            loadFriendRequests();
        }
    }, 30000); // 30秒
}

// 停止定时刷新
function stopFriendRequestRefresh() {
    if (friendRequestRefreshTimer) {
        clearInterval(friendRequestRefreshTimer);
        friendRequestRefreshTimer = null;
    }
}
