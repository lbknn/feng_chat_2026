package org.example.fengbushi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 群聊信息DTO - 包含成员数量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupInfoDTO {
    private Long groupId;
    private String groupName;
    private Long ownerId;
    private String announcement;
    private String avatar;
    private String createdAt;
    private Long memberCount; // 成员数量
}
