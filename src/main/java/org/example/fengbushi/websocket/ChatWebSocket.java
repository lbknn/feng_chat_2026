package org.example.fengbushi.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.fengbushi.entity.mongodb.Message;
import org.example.fengbushi.service.ConversationService;
import org.example.fengbushi.service.CrossServerClient;
import org.example.fengbushi.service.DistributedMessageService;
import org.example.fengbushi.service.GroupService;
import org.example.fengbushi.service.MessageService;
import org.example.fengbushi.service.UserSessionService;
import org.example.fengbushi.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务端点
 * 用于实时消息推送
 */
@Slf4j
@Component
@ServerEndpoint("/ws/chat/{userId}")
public class ChatWebSocket {
    
    // 存储所有在线用户的WebSocket会话
    private static final Map<Long, Session> ONLINE_USERS = new ConcurrentHashMap<>();
    
    private static ObjectMapper objectMapper = new ObjectMapper();
    
    // Spring Bean注入（通过Setter）
    private static MessageService messageService;
    private static ConversationService conversationService;
    private static UserSessionService userSessionService;
    private static JwtUtil jwtUtil;
    private static DistributedMessageService distributedMessageService;
    
    @Autowired
    public void setMessageService(MessageService messageService) {
        ChatWebSocket.messageService = messageService;
        log.info("MessageService 已成功注入 WebSocket");
    }
    
    @Autowired
    public void setConversationService(ConversationService conversationService) {
        ChatWebSocket.conversationService = conversationService;
        log.info("ConversationService 已成功注入 WebSocket");
    }
    
    @Autowired
    public void setUserSessionService(UserSessionService userSessionService) {
        ChatWebSocket.userSessionService = userSessionService;
        log.info("UserSessionService 已成功注入 WebSocket");
    }
    
    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        ChatWebSocket.jwtUtil = jwtUtil;
        log.info("JwtUtil 已成功注入 WebSocket");
    }
    
    @Autowired
    public void setDistributedMessageService(DistributedMessageService distributedMessageService) {
        ChatWebSocket.distributedMessageService = distributedMessageService;
        log.info("DistributedMessageService 已成功注入 WebSocket");
    }
    
    /**
     * 连接建立成功调用
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId, EndpointConfig config) {
        // 🔥 验证 Token
        String queryString = session.getQueryString();
        if (queryString != null && queryString.contains("token=")) {
            String token = queryString.split("token=")[1].split("&")[0];
            if (jwtUtil != null && !jwtUtil.validateToken(token)) {
                log.warn("⚠️ 用户{}连接失败：Token无效, token前20字符: {}", userId, token.substring(0, Math.min(20, token.length())));
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid Token"));
                } catch (IOException e) {
                    log.error("关闭会话失败", e);
                }
                return;
            }
        }
        
        ONLINE_USERS.put(userId, session);
        log.info("✅ 用户{}已连接，当前在线人数: {}", userId, ONLINE_USERS.size());
        
        // 🔥 在Redis中注册用户会话(用于分布式路由)
        if (userSessionService != null) {
            try {
                String serverAddress = System.getenv("SERVER_ADDRESS");
                if (serverAddress == null || serverAddress.isEmpty()) {
                    // 如果环境变量未设置,使用本地IP
                    try {
                        String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                        String port = System.getProperty("server.port", "8080");
                        serverAddress = ip + ":" + port;
                    } catch (Exception e) {
                        serverAddress = "localhost:8080";
                    }
                }
                userSessionService.registerUserServer(userId, serverAddress);
                log.info("🔗 用户在Redis中注册成功: userId={}, server={}", userId, serverAddress);
            } catch (Exception e) {
                log.error("❌ Redis注册失败，但不影响本地通信: userId={}", userId, e);
            }
        }
        
        // 发送连接成功消息
        sendMessage(session, new WsMessage("connected", "连接成功", null));
        
        // 🔥 关键修复：用户上线时，推送所有会话的离线消息
        try {
            var ctx = org.example.fengbushi.FengbushiApplication.getApplicationContext();
            if (ctx != null) {
                ConversationService conversationService = ctx.getBean(ConversationService.class);
                MessageService messageService = ctx.getBean(MessageService.class);
                CrossServerClient crossServerClient = ctx.getBean(CrossServerClient.class);
                
                // 获取用户的所有会话
                var conversations = conversationService.getConversations(userId);
                log.info("📬 用户{}上线，共有{}个会话", userId, conversations.size());
                
                // 遍历每个会话，查询未读消息并推送
                for (var conv : conversations) {
                    Long targetId = conv.getTargetId();
                    String convType = conv.getType();
                    
                    // 🔥 计算正确的 convId
                    Long convId;
                    if ("friend".equals(convType)) {
                        // 单聊：convId = min(userId, targetId) * 100000 + max(userId, targetId)
                        convId = Math.min(userId, targetId) * 100000L + Math.max(userId, targetId);
                    } else {
                        // 群聊：convId = groupId
                        convId = targetId;
                    }
                    
                    log.info("🔍 处理会话: targetId={}, type={}, 计算后的convId={}", targetId, convType, convId);
                    
                    // 1️⃣ 查询本地未读消息
                    var localMessages = messageService.getLocalUnreadMessages(convId, userId);
                    log.info("🔍 会话{}查询到{}条本地未读消息", convId, localMessages.size());
                    
                    // 🔥 调试：查询该会话的所有消息
                    var allMsgs = messageService.getAllMessages(convId);
                    if (!allMsgs.isEmpty()) {
                        log.info("🔎 会话{}共有{}条消息，前3条详情：", convId, allMsgs.size());
                        int count = 0;
                        for (var m : allMsgs) {
                            if (count++ >= 3) break;
                            log.info("   📝 msgId={}, senderId={}, isRead={}, serverAddress={}, isLocationRecord={}, sendTime={}",
                                    m.getMsgId(), m.getSenderId(), m.isRead(), m.getServerAddress(), m.isLocationRecord(), m.getSendTime());
                        }
                    }
                    
                    // 2️⃣ 查询外地消息位置
                    var remoteLocations = messageService.getRemoteMessageLocations(convId, userId);
                    log.info("🌐 会话{}查询到{}条外地消息位置", convId, remoteLocations.size());
                    
                    List<Message> allUnreadMessages = new java.util.ArrayList<>(localMessages);
                    
                    // 3️⃣ 从远程服务器获取外地消息
                    for (var location : remoteLocations) {
                        String serverAddress = location.getServerAddress();
                        String msgId = location.getMsgId();
                        
                        if (serverAddress != null && msgId != null) {
                            log.info("🌐 从服务器 {} 获取消息: msgId={}", serverAddress, msgId);
                            Message remoteMessage = crossServerClient.fetchMessageFromServer(serverAddress, msgId);
                            
                            if (remoteMessage != null) {
                                allUnreadMessages.add(remoteMessage);
                                log.info("✅ 成功获取外地消息: msgId={}", msgId);
                                
                                // 🔥 广播删除位置记录
                                distributedMessageService.broadcastDeleteOfflineMessage(msgId);
                                log.info("📢 已广播删除位置记录: msgId={}", msgId);
                            } else {
                                log.warn("⚠️ 获取外地消息失败: msgId={}", msgId);
                                messageService.deleteLocationRecordForUser(msgId, userId);
                            }
                        }
                    }
                    
                    // 4️⃣ 推送所有未读消息
                    if (!allUnreadMessages.isEmpty()) {
                        log.info("📨 会话{}共推送{}条未读消息（本地{}+外地{})", convId, allUnreadMessages.size(), localMessages.size(), remoteLocations.size());
                        
                        // 逐条推送未读消息
                        for (var msg : allUnreadMessages) {
                            WsMessage chatMsg = new WsMessage();
                            chatMsg.setType("chat");
                            
                            Map<String, Object> data = new ConcurrentHashMap<>();
                            data.put("senderId", msg.getSenderId());
                            data.put("receiverId", convId);
                            data.put("content", msg.getContent());
                            data.put("msgType", msg.getMsgType() != null ? msg.getMsgType() : "text");
                            data.put("timestamp", msg.getSendTime().toString());
                            data.put("conversationType", convType);
                            data.put("convId", convId);
                            data.put("msgId", msg.getMsgId());
                            if (msg.getExtra() != null) {
                                data.put("extra", msg.getExtra());
                            }
                            chatMsg.setData(data);
                            
                            sendMessage(session, chatMsg);
                            
                            // 正文可能只在对方 Mongo：本地有文档才标记已读
                            messageService.markMessageAsReadIfPresent(msg.getMsgId());
                        }
                        
                        // 清除会话未读数（第二参数是会话的 targetId，即好友/群 ID，不是 Mongo 会话 convId）
                        conversationService.clearUnreadCount(userId, targetId, convType);
                    }
                }
                
                log.info("✅ 用户{}离线消息推送完成", userId);
            }
        } catch (Exception e) {
            log.error("❌ 推送离线消息失败", e);
        }
    }
    
    /**
     * 连接关闭调用
     */
    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        ONLINE_USERS.remove(userId);
        log.info("❌ 用户{}已断开，当前在线人数: {}", userId, ONLINE_USERS.size());
        
        // 🔥 从Redis中注销用户会话
        if (userSessionService != null) {
            try {
                userSessionService.unregisterUserServer(userId);
                log.info("🔌 用户从Redis中注销成功: userId={}", userId);
            } catch (Exception e) {
                log.error("❌ Redis注销失败: userId={}", userId, e);
            }
        }
    }
    
    /**
     * 收到客户端消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") Long userId) {
        log.info("📥 收到用户{}的消息: {}", userId, message);
        
        try {
            // 解析消息
            Map<String, Object> msgMap = objectMapper.readValue(message, Map.class);
            String msgType = (String) msgMap.get("type");
            log.info("🔍 消息类型: {}, 所有字段: {}", msgType, msgMap.keySet());
            
            if ("chat".equals(msgType)) {
                Long receiverId = null;
                if (msgMap.get("receiverId") instanceof Number) {
                    receiverId = ((Number) msgMap.get("receiverId")).longValue();
                }
                
                String content = (String) msgMap.get("content");
                String conversationType = (String) msgMap.get("conversationType");
                String timestamp = (String) msgMap.get("timestamp");
                String msgTypeField = (String) msgMap.getOrDefault("msgType", "text");
                String extra = (String) msgMap.get("extra");
                Object fileSize = msgMap.get("fileSize");
                boolean ephemeral = Boolean.parseBoolean(String.valueOf(msgMap.getOrDefault("ephemeral", "false")));
                String clientMsgId = (String) msgMap.get("msgId"); // 🔥 客户端消息ID
                
                if (receiverId != null && content != null) {
                    var ctx = org.example.fengbushi.FengbushiApplication.getApplicationContext();
                    if (ctx == null) {
                        log.error("❌ ApplicationContext 为空");
                        return;
                    }
                    
                    MessageService messageService = ctx.getBean(MessageService.class);
                    ConversationService conversationService = ctx.getBean(ConversationService.class);
                    GroupService groupService = ctx.getBean(GroupService.class);
                    
                    if ("group".equals(conversationType)) {
                        handleGroupMessage(userId, receiverId, content, msgTypeField, extra, fileSize, timestamp, clientMsgId, ephemeral,
                                         messageService, conversationService, groupService);
                    } else {
                        handleSingleMessage(userId, receiverId, content, msgTypeField, extra, fileSize, timestamp, clientMsgId, ephemeral,
                                          messageService, conversationService);
                    }
                }
            } else if ("ack".equals(msgType)) {
                // 🔥 处理消息确认
                String msgId = (String) msgMap.get("msgId");
                Long senderId = null;
                if (msgMap.get("senderId") instanceof Number) {
                    senderId = ((Number) msgMap.get("senderId")).longValue();
                }
                
                if (msgId != null && senderId != null) {
                    handleAckMessage(msgId, senderId, userId);
                }
            } else if ("friend_request".equals(msgType)) {
                // 处理好友申请消息
                log.info("✅ 检测到好友申请消息类型，准备调用 handleFriendRequest");
                handleFriendRequest(userId, msgMap);
            } else {
                log.warn("⚠️ 未识别的消息类型: {}", msgType);
            }
        } catch (Exception e) {
            log.error("❌ 处理消息异常", e);
        }
    }
    
    /**
     * 处理单聊消息
     */
    private void handleSingleMessage(Long senderId, Long receiverId, String content, String msgType,
                                     String extra, Object fileSize, String timestamp, String clientMsgId, boolean ephemeral,
                                     MessageService messageService, ConversationService conversationService) {
        log.info("🔵 开始处理单聊消息: senderId={}, receiverId={}, msgType={}, clientMsgId={}", senderId, receiverId, msgType, clientMsgId);
        
        Long convId = Math.min(senderId, receiverId) * 100000L + Math.max(senderId, receiverId);
        
        try {
            log.info("💾 开始保存消息到MongoDB: convId={}, senderId={}, msgType={}", convId, senderId, msgType);
            Message savedMessage = messageService.sendMessageWithExtra(convId, senderId, msgType, content, extra, ephemeral);
            log.info("✅ 消息保存成功: messageId={}", savedMessage.getMsgId());
            
            String displayContent = "file".equals(msgType) ? "[文件]" : content;
            conversationService.createOrUpdateConversation(senderId, receiverId, "friend", displayContent);
            conversationService.createOrUpdateConversation(receiverId, senderId, "friend", displayContent);
            conversationService.incrementUnreadCount(receiverId, senderId, "friend");
            
            // 🔥 立即向发送者确认消息已存储到数据库
            WsMessage storageAck = new WsMessage();
            storageAck.setType("message");
            
            Map<String, Object> storageData = new ConcurrentHashMap<>();
            storageData.put("msgId", clientMsgId); // 客户端消息ID
            storageData.put("serverMsgId", savedMessage.getMsgId()); // 服务器消息ID
            storageData.put("stored", true); // 标记为已存储
            storageAck.setData(storageData);
            
            log.info("📦 准备发送存储确认: senderId={}, clientMsgId={}, stored=true", senderId, clientMsgId);
            log.info("📦 存储确认数据结构: {}", objectMapper.writeValueAsString(storageAck));
            boolean sent = sendToUser(senderId, storageAck);
            log.info("📥 发送存储确认结果: senderId={}, success={}", senderId, sent);
            
            if (sent) {
                log.info("✅ 已向发送者{}确认消息已存储: clientMsgId={}", senderId, clientMsgId);
            } else {
                log.warn("⚠️ 发送者{}不在线，无法发送存储确认: clientMsgId={}", senderId, clientMsgId);
            }
            
            // 转发给接收者
            WsMessage forwardMsg = new WsMessage();
            forwardMsg.setType("chat");
            
            Map<String, Object> data = new ConcurrentHashMap<>();
            data.put("senderId", senderId);
            data.put("receiverId", receiverId);
            data.put("content", content);
            data.put("msgType", msgType != null ? msgType : "text");
            if (extra != null) {
                data.put("extra", extra);
            }
            if (fileSize != null) {
                data.put("fileSize", fileSize);
            }
            if (timestamp != null) {
                data.put("timestamp", timestamp);
            }
            // 🔥 添加客户端消息ID，用于确认机制
            if (clientMsgId != null) {
                data.put("msgId", clientMsgId);
            }
            data.put("conversationType", "friend");
            data.put("convId", convId);
            forwardMsg.setData(data);
            
            sendToUser(receiverId, forwardMsg);
            log.info("📤 单聊消息已转发: {} -> {}", senderId, receiverId);
            
            // 🔥 关键修复：如果接收者不在线，广播离线消息位置
            if (!isOnline(receiverId)) {
                distributedMessageService.broadcastOfflineMessage(receiverId, savedMessage.getMsgId(), convId);
                log.info("📢 广播离线消息位置: userId={}, msgId={}, convId={}", receiverId, savedMessage.getMsgId(), convId);
            }
            // 注意：不再立即标记为已读，等待用户重新连接时通过推送逻辑标记
            
            // 🔥 关键修复：单聊消息也必须保留在MongoDB中，不能删除！
            // 之前删除导致：1.发送方显示失败 2.刷新后消息消失
            // deleteMessageAfterForward(savedMessage.getMsgId(), receiverId);
            log.info("💾 单聊消息保留在MongoDB: msgId={}", savedMessage.getMsgId());
        } catch (Exception e) {
            log.error("❌ 处理单聊消息失败", e);
        }
    }
    
    /**
     * 处理群聊消息
     */
    private void handleGroupMessage(Long senderId, Long groupId, String content, String msgType,
                                    String extra, Object fileSize, String timestamp, String clientMsgId, boolean ephemeral,
                                    MessageService messageService, ConversationService conversationService,
                                    GroupService groupService) {
        log.info("🔵 开始处理群聊消息: senderId={}, groupId={}, content={}", senderId, groupId, content);
        
        try {
            var members = groupService.getGroupMembers(groupId);
            log.info("🔵 群 {} 的成员数量: {}", groupId, members.size());
            log.info("🔵 群成员列表: {}", members.stream().map(m -> m.getUserId()).collect(java.util.stream.Collectors.toList()));
            
            if (members.isEmpty()) {
                log.warn("⚠️ 群 {} 没有成员", groupId);
                return;
            }
            
            log.info("💾 开始保存消息到MongoDB: groupId={}, senderId={}", groupId, senderId);
            Message savedMessage = messageService.sendMessageWithExtra(groupId, senderId, msgType, content, extra, ephemeral);
            log.info("✅ 消息保存成功: messageId={}", savedMessage.getMsgId());
            
            String displayContent = "file".equals(msgType) ? "[文件]" : content;
            for (var member : members) {
                Long memberId = member.getUserId();
                if (!memberId.equals(senderId)) {
                    log.info("📝 更新用户 {} 的会话和未读数", memberId);
                    conversationService.createOrUpdateConversation(memberId, groupId, "group", displayContent);
                    conversationService.incrementUnreadCount(memberId, groupId, "group");
                }
            }
            
            // 🔥 立即向发送者确认消息已存储到数据库
            WsMessage storageAck = new WsMessage();
            storageAck.setType("message");
            
            Map<String, Object> storageData = new ConcurrentHashMap<>();
            storageData.put("msgId", clientMsgId); // 客户端消息ID
            storageData.put("serverMsgId", savedMessage.getMsgId()); // 服务器消息ID
            storageData.put("stored", true); // 标记为已存储
            storageAck.setData(storageData);
            
            sendToUser(senderId, storageAck);
            log.info("📥 已向发送者确认群消息已存储: senderId={}, groupId={}, clientMsgId={}", senderId, groupId, clientMsgId);
            
            // 转发给所有群成员
            WsMessage forwardMsg = new WsMessage();
            forwardMsg.setType("chat");
            
            Map<String, Object> data = new ConcurrentHashMap<>();
            data.put("senderId", senderId);
            data.put("receiverId", groupId);
            data.put("content", content);
            data.put("msgType", msgType != null ? msgType : "text");
            if (extra != null) {
                data.put("extra", extra);
            }
            if (fileSize != null) {
                data.put("fileSize", fileSize);
            }
            if (timestamp != null) {
                data.put("timestamp", timestamp);
            }
            // 🔥 添加客户端消息ID，用于确认机制
            if (clientMsgId != null) {
                data.put("msgId", clientMsgId);
            }
            data.put("conversationType", "group");
            data.put("convId", groupId);
            forwardMsg.setData(data);
            
            log.info("📦 准备转发群消息,数据结构: {}", objectMapper.writeValueAsString(data));
            
            int sentCount = 0;
            for (var member : members) {
                Long memberId = member.getUserId();
                if (!memberId.equals(senderId)) {
                    log.info("📤 尝试发送消息给用户 {}", memberId);
                    boolean isOnline = isOnline(memberId);
                    log.info("📡 用户 {} 在线状态: {}", memberId, isOnline);
                    
                    if (isOnline) {
                        sendToUser(memberId, forwardMsg);
                        sentCount++;
                        log.info("✅ 成功发送给用户 {}", memberId);
                    } else {
                        log.warn("⚠️ 用户 {} 不在线,广播离线消息", memberId);
                        distributedMessageService.broadcastOfflineMessage(memberId, savedMessage.getMsgId(), groupId);
                    }
                }
            }
            
            log.info("📤 群聊消息已转发: 发送者={}, 群ID={}, 接收人数={}", senderId, groupId, sentCount);
            
            // 🔥 关键修复：群聊消息不应该立即删除，需要保留在数据库中供后续查询
            // 只有单聊的临时中转消息才需要删除
            // deleteMessageAfterForward(savedMessage.getMsgId(), null); // 注释掉这行
            log.info("💾 群聊消息已保存到MongoDB，不删除: msgId={}", savedMessage.getMsgId());
        } catch (Exception e) {
            log.error("❌ 处理群聊消息失败", e);
        }
    }
    
    /**
     * 连接错误调用
     */
    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") Long userId) {
        log.error("WebSocket错误，用户ID: {}", userId, error);
        onClose(userId);
    }
    
    /**
     * 向指定用户发送消息
     * @return 是否发送成功
     */
    public static boolean sendToUser(Long userId, WsMessage wsMessage) {
        Session session = ONLINE_USERS.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, wsMessage);
            return true;
        }
        
        // 🔥 用户不在本地服务器，尝试跨服务器转发
        if (distributedMessageService != null && userSessionService != null) {
            // 🔥 关键修复：先检查 Redis 中用户是否在其他服务器
            String userServer = userSessionService.getUserServer(userId);
            
            if (userServer != null) {
                // 用户在其他服务器，才需要跨服务器转发
                String currentServer = getCurrentServerAddress();
                if (!currentServer.equals(userServer)) {
                    log.info("🔄 用户{}在其他服务器 {}，尝试跨服务器转发", userId, userServer);
                    distributedMessageService.sendToRemoteUser(userId, wsMessage);
                    return true;
                } else {
                    // 用户在本服务器但不在线（可能刚断开）
                    log.warn("⚠️ 用户{}在本服务器但不在线", userId);
                    return false;
                }
            }
            
            // 用户不在任何服务器
            log.warn("⚠️ 用户{}不在线或连接已关闭", userId);
            return false;
        }
        
        log.warn("⚠️ 用户{}不在线或连接已关闭", userId);
        return false;
    }
    
    /**
     * 获取当前服务器地址
     */
    private static String getCurrentServerAddress() {
        String addr = System.getenv("SERVER_ADDRESS");
        if (addr == null || addr.isEmpty()) {
            try {
                String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                String port = System.getProperty("server.port", "8080");
                addr = ip + ":" + port;
            } catch (Exception e) {
                addr = "localhost:8080";
            }
        }
        return addr;
    }
    
    /**
     * 广播消息给所有在线用户
     */
    public static void broadcast(WsMessage wsMessage) {
        ONLINE_USERS.forEach((userId, session) -> {
            if (session.isOpen()) {
                sendMessage(session, wsMessage);
            }
        });
    }
    
    /**
     * 发送消息到会话
     */
    private static void sendMessage(Session session, WsMessage wsMessage) {
        try {
            String json = objectMapper.writeValueAsString(wsMessage);
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }
    
    /**
     * 获取在线用户数
     */
    public static int getOnlineCount() {
        return ONLINE_USERS.size();
    }
    
    /**
     * 检查用户是否在线
     */
    public static boolean isOnline(Long userId) {
        return ONLINE_USERS.containsKey(userId);
    }
    
    /**
     * 🔥 消息转发后删除MongoDB中的记录
     * @param msgId 消息ID
     * @param receiverId 接收者ID(单聊时使用,群聊传null)
     */
    private void deleteMessageAfterForward(String msgId, Long receiverId) {
        try {
            var ctx = org.example.fengbushi.FengbushiApplication.getApplicationContext();
            if (ctx == null) {
                log.warn("⚠️ ApplicationContext 为空，无法删除消息");
                return;
            }
            
            var messageRepository = ctx.getBean(org.example.fengbushi.repository.mongodb.MessageRepository.class);
            
            // 🔥 先检查消息是否存在
            boolean exists = messageRepository.existsById(msgId);
            log.info("🔍 删除前检查: msgId={}, 是否存在={}", msgId, exists);
            
            if (exists) {
                messageRepository.deleteById(msgId);
                
                // 🔥 再次检查是否删除成功
                boolean stillExists = messageRepository.existsById(msgId);
                log.info("️ 删除操作完成: msgId={}, 是否仍存在={}", msgId, stillExists);
                
                if (!stillExists) {
                    if (receiverId != null) {
                        log.info("🗑️ 单聊消息已从MongoDB删除: msgId={}, receiverId={}", msgId, receiverId);
                    } else {
                        log.info("🗑️ 群聊消息已从MongoDB删除: msgId={}", msgId);
                    }
                } else {
                    log.error("❌ 消息删除失败，仍然存在: msgId={}", msgId);
                }
            } else {
                log.warn("⚠️ 消息不存在，无需删除: msgId={}", msgId);
            }
        } catch (Exception e) {
            log.error("❌ 删除消息失败: msgId={}", msgId, e);
        }
    }
    
    /**
     * 🔥 处理消息确认（接收者已收到消息）
     * @param msgId 客户端消息ID
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     */
    private void handleAckMessage(String msgId, Long senderId, Long receiverId) {
        log.info("🔵 收到消息确认: msgId={}, senderId={}, receiverId={}", msgId, senderId, receiverId);
        
        try {
            // 构造确认响应消息
            WsMessage ackResponse = new WsMessage();
            ackResponse.setType("message");
            
            Map<String, Object> data = new ConcurrentHashMap<>();
            data.put("msgId", msgId);
            data.put("ack", true); // 标记为确认消息
            ackResponse.setData(data);
            
            // 发送给原始发送者
            sendToUser(senderId, ackResponse);
            log.info("✅ 已通知发送者消息送达: msgId={}, senderId={}", msgId, senderId);
            
            // 注意：MongoDB中的消息已经在转发时删除了，此处无需再次删除
        } catch (Exception e) {
            log.error("❌ 处理消息确认失败", e);
        }
    }
    
    /**
     * 处理好友申请消息
     * @param fromUserId 申请人ID
     * @param msgMap 消息数据
     */
    private void handleFriendRequest(Long fromUserId, Map<String, Object> msgMap) {
        log.info("🔵 ========== 收到好友申请 ==========");
        log.info("🔵 fromUserId: {}", fromUserId);
        log.info("🔵 msgMap keys: {}", msgMap.keySet());
        log.info("🔵 msgMap content: {}", msgMap);
        
        try {
            // 获取目标用户ID
            Long toUserId = null;
            if (msgMap.get("targetId") instanceof Number) {
                toUserId = ((Number) msgMap.get("targetId")).longValue();
            }
            
            log.info("🔵 解析到的 targetId: {}", toUserId);
            
            String content = (String) msgMap.get("content");
            String extra = (String) msgMap.get("extra");
            
            log.info("🔵 content: {}", content);
            log.info("🔵 extra: {}", extra);
            
            if (toUserId != null) {
                // 计算会话ID
                Long convId = Math.min(fromUserId, toUserId) * 100000L + Math.max(fromUserId, toUserId);
                log.info("🔵 计算的 convId: {}", convId);
                
                // 保存到MongoDB（不转发）
                var ctx = org.example.fengbushi.FengbushiApplication.getApplicationContext();
                if (ctx != null) {
                    MessageService messageService = ctx.getBean(MessageService.class);
                    messageService.sendMessageWithExtra(convId, fromUserId, "friend_request", content, extra, false);
                    log.info("✅ 好友申请已保存到MongoDB: convId={}, from={}, to={}", convId, fromUserId, toUserId);
                } else {
                    log.error("❌ ApplicationContext 为空，无法保存好友申请");
                }
                
                // 🔥 如果目标用户在线，发送通知（不包含申请内容，仅提示有新申请）
                boolean isOnline = isOnline(toUserId);
                log.info("🔵 目标用户 {} 在线状态: {}", toUserId, isOnline);
                
                if (isOnline) {
                    WsMessage notifyMsg = new WsMessage();
                    notifyMsg.setType("notification");
                    notifyMsg.setContent("您有新的朋友申请");
                    boolean sent = sendToUser(toUserId, notifyMsg);
                    log.info("🔔 发送通知给用户{} 结果: {}", toUserId, sent ? "成功" : "失败");
                } else {
                    log.info("🔵 目标用户 {} 不在线，无法发送通知", toUserId);
                }
            } else {
                log.error("❌ targetId 为 null，无法处理好友申请");
            }
        } catch (Exception e) {
            log.error("❌ 处理好友申请失败", e);
        }
        
        log.info("🔵 ==========================================");
    }
    
    /**
     * WebSocket消息封装
     */
    @Data
    public static class WsMessage {
        private String type;      // 消息类型：chat/notification/system等
        private String content;   // 消息内容
        private Object data;      // 附加数据
        
        public WsMessage() {}
        
        public WsMessage(String type, String content, Object data) {
            this.type = type;
            this.content = content;
            this.data = data;
        }
    }
}




