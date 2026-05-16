// WebSocket连接管理
class WebSocketManager {
    constructor() {
        this.ws = null;
        this.userId = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000; // 3秒后重连
        this.messageHandlers = [];
    }

    // 连接WebSocket
    connect(userId) {
        this.userId = userId;
        
        const token = localStorage.getItem('token');
        if (!token) {
            console.error('❌ 未找到认证Token，请先登录');
            return;
        }
        
        const wsUrl = `ws://192.168.50.100/ws/chat/${userId}?token=${token}`;
        
        console.log('🔌 正在连接WebSocket:', wsUrl);
        console.log('📍 当前页面URL:', window.location.href);
        console.log('🌐 当前主机名:', window.location.hostname);
        
        try {
            this.ws = new WebSocket(wsUrl);
            
            this.ws.onopen = (event) => {
                console.log('✅ WebSocket连接成功');
                this.reconnectAttempts = 0;
                this.notifyHandlers('connected', { message: '连接成功' });
            };
            
            this.ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    console.log('📨 WebSocket收到消息:', data.type, data);
                    this.handleMessage(data);
                } catch (error) {
                    console.error('解析消息失败:', error);
                }
            };
            
            this.ws.onerror = (error) => {
                console.error('❌ WebSocket错误:', error);
                console.error(`请检查后端服务是否启动在 ws://${window.location.hostname}:8080`);
                this.notifyHandlers('error', { error });
            };
            
            this.ws.onclose = (event) => {
                console.log('🔌 WebSocket连接关闭:', event.code, event.reason);
                
                // 🔥 检测 Token 过期或无效
                if (event.code === 1008 || (event.reason && event.reason.includes('Invalid Token'))) {
                    console.warn('⚠️ Token 已过期或无效，跳转到登录页');
                    localStorage.removeItem('token');
                    localStorage.removeItem('userId');
                    window.location.href = '/index.html';
                    return;
                }
                
                this.notifyHandlers('disconnected', { code: event.code });
                
                // 尝试重连
                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnectAttempts++;
                    console.log(`⏳ ${this.reconnectDelay / 1000}秒后尝试重连... (尝试次数: ${this.reconnectAttempts})`);
                    setTimeout(() => this.connect(this.userId), this.reconnectDelay);
                } else {
                    console.error('❌ 达到最大重连次数,请刷新页面或检查后端服务');
                    this.notifyHandlers('reconnect_failed', {});
                }
            };
        } catch (error) {
            console.error('❌ 创建WebSocket连接失败:', error);
        }
    }

    // 处理收到的消息
    handleMessage(data) {
        switch (data.type) {
            case 'connected':
                console.log('服务器确认连接');
                this.notifyHandlers('connected', data);
                break;
            case 'chat':
                // 聊天消息，data.data 中包含详细信息
                this.notifyHandlers('message', data.data || data);
                break;
            case 'message':
                // 🔥 关键修复：处理服务器存储确认消息
                this.notifyHandlers('message', data.data || data);
                break;
            case 'notification':
                this.notifyHandlers('notification', data);
                break;
            default:
                console.log('未知消息类型:', data.type);
        }
    }

    // 发送消息
    send(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
            return true;
        } else {
            console.warn('WebSocket未连接,无法发送消息');
            return false;
        }
    }

    // 断开连接
    disconnect() {
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
    }

    // 注册消息处理器
    onMessage(handler) {
        this.messageHandlers.push(handler);
    }

    // 移除消息处理器
    offMessage(handler) {
        const index = this.messageHandlers.indexOf(handler);
        if (index > -1) {
            this.messageHandlers.splice(index, 1);
        }
    }

    // 通知所有处理器
    notifyHandlers(type, data) {
        this.messageHandlers.forEach(handler => {
            try {
                handler(type, data);
            } catch (error) {
                console.error('消息处理器执行错误:', error);
            }
        });
    }

    // 检查连接状态
    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }
}

// 创建全局WebSocket实例
const wsManager = new WebSocketManager();
