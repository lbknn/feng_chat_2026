// API配置
const API_BASE_URL = 'http://192.168.50.100:80';

console.log('🔧 API_BASE_URL:', API_BASE_URL);

// API工具类
class ApiClient {
    constructor() {
        this.token = localStorage.getItem('token');
    }

    // 设置认证token
    setToken(token) {
        this.token = token;
        localStorage.setItem('token', token);
    }

    // 清除token
    clearToken() {
        this.token = null;
        localStorage.removeItem('token');
    }

    // 通用请求方法
    async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        if (this.token) {
            config.headers['Authorization'] = `Bearer ${this.token}`;
        }

        try {
            const response = await fetch(url, config);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('API请求失败:', error);
            throw error;
        }
    }

    // GET请求
    async get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    }

    // POST请求
    async post(endpoint, data) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    // PUT请求
    async put(endpoint, data) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    // DELETE请求
    async delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }

    // 文件上传请求
    async uploadFile(file) {
        const url = `${API_BASE_URL}/api/file/upload`;
        const formData = new FormData();
        formData.append('file', file);
        
        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${this.token}`
                },
                body: formData
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('文件上传失败:', error);
            throw error;
        }
    }

    // ========== 用户相关API ==========
    
    // 登录
    async login(username, password) {
        return this.post('/api/user/login', { username, password });
    }

    // 注册
    async register(username, nickname, password) {
        return this.post('/api/user/register', { username, nickname, password });
    }

    // 获取用户信息
    async getUserInfo(userId) {
        return this.get(`/api/user/${userId}`);
    }

    // ========== 好友相关API ==========
    
    // 获取好友列表
    async getFriends(userId) {
        return this.get(`/api/friend/list/${userId || currentUser?.userId}`);
    }

    // 添加好友
    async addFriend(friendId, remark) {
        const params = new URLSearchParams({
            userId: currentUser?.userId,
            friendId: friendId
        });
        if (remark) {
            params.append('remark', remark);
        }
        return this.post(`/api/friend/add?${params.toString()}`, {});
    }

    // 删除好友
    async removeFriend(friendId) {
        return this.delete(`/api/friend/delete?userId=${currentUser?.userId}&friendId=${friendId}`);
    }

    // 设置好友备注
    async setRemark(friendId, remark) {
        const params = new URLSearchParams({
            userId: currentUser?.userId,
            friendId: friendId,
            remark: remark
        });
        return this.put(`/api/friend/remark?${params.toString()}`, {});
    }

    // 清除未读数
    async clearUnreadCount(userId, targetId, type) {
        const params = new URLSearchParams({
            userId: userId,
            targetId: targetId,
            type: type
        });
        return this.put(`/api/conversation/clear-unread?${params.toString()}`, {});
    }

    // 更新用户信息
    async updateUserInfo(userId, data) {
        const params = new URLSearchParams();
        params.append('userId', userId);
        if (data.nickname) params.append('nickname', data.nickname);
        if (data.avatar) params.append('avatar', data.avatar);
        if (data.signature) params.append('signature', data.signature);
        if (data.email) params.append('email', data.email);
        if (data.phone) params.append('phone', data.phone);
        return this.put(`/api/user/update?${params.toString()}`, {});
    }

    // 获取好友详细信息（包括备注）
    async getFriendInfo(userId, friendId) {
        return this.get(`/api/friend/info?userId=${userId}&friendId=${friendId}`);
    }

    // 获取好友申请列表
    async getFriendRequests(userId) {
        return this.get(`/api/friend/requests/${userId || currentUser?.userId}`);
    }
    
    // 拒绝好友申请
    async rejectFriendRequest(userId, fromUserId) {
        return this.delete(`/api/friend/reject?userId=${userId}&fromUserId=${fromUserId}`);
    }

    // ========== 群聊相关API ==========
    
    // 获取群聊列表
    async getGroups(userId) {
        const params = userId ? `?userId=${userId}` : '';
        return this.get(`/api/group/list${params}`);
    }

    // 创建群聊
    async createGroup(ownerId, groupName, avatar, memberIds) {
        return this.post('/api/group/create', { ownerId, groupName, avatar, memberIds });
    }

    // 获取群成员
    async getGroupMembers(groupId) {
        return this.get(`/api/group/members/${groupId}`);
    }
    
    // 获取群详情
    async getGroupDetail(groupId) {
        return this.get(`/api/group/${groupId}/detail`);
    }
    
    // 更新群信息
    async updateGroup(groupId, groupName, avatar, announcement) {
        const params = new URLSearchParams();
        params.append('groupId', groupId);
        if (groupName) params.append('groupName', groupName);
        if (avatar) params.append('avatar', avatar);
        if (announcement) params.append('announcement', announcement);
        return this.put(`/api/group/update?${params.toString()}`, {});
    }
    
    // 移除群成员
    async removeGroupMember(groupId, userId) {
        return this.delete(`/api/group/member/remove?groupId=${groupId}&userId=${userId}`);
    }
    
    // 修改群成员职务
    async updateGroupMemberRole(groupId, userId, role) {
        return this.put(`/api/group/member/role?groupId=${groupId}&userId=${userId}&role=${role}`, {});
    }

    // ========== 消息相关API ==========
    
    // 获取历史消息
    async getMessages(conversationId, page = 0, size = 50, unreadCount = null, onlyUnread = false, userId = null) {
        let url = `/api/message/list/${conversationId}?page=${page}&size=${size}&onlyUnread=${onlyUnread}`;
        if (unreadCount !== null) {
            url += `&unreadCount=${unreadCount}`;
        }
        if (userId !== null) {
            url += `&userId=${userId}`;
        }
        return this.get(url);
    }
    
    // 标记所有消息为已读
    async markAllMessagesAsRead(convId, userId) {
        return this.put(`/api/message/read-all/${convId}?userId=${userId}`, {});
    }

    // 发送消息(通过HTTP,WebSocket失败时的备选)
    async sendMessage(conversationId, content, type = 'text') {
        return this.post('/api/message/send', { conversationId, content, type });
    }

    // ========== 会话相关API ==========
    
    // 获取会话列表
    async getConversations(userId) {
        const params = userId ? `?userId=${userId}` : '';
        return this.get(`/api/conversation/list${params}`);
    }
}

// 创建全局API实例
const api = new ApiClient();
