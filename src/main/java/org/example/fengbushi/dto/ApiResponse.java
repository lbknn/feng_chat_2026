package org.example.fengbushi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 包装。反序列化时忽略未知字段，并兼容部分节点返回的 {@code success} 布尔字段，
 * 以便跨服 {@link org.example.fengbushi.service.CrossServerClient} 解析不同版本 JSON。
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;

    /**
     * 其它实例可能返回的冗余成功标记；本服务序列化时默认不输出。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean success;

    public ApiResponse(Integer code, String message, T data, Boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, null);
    }
    
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null, null);
    }
    
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null, null);
    }
    
    public boolean isSuccess() {
        if (Boolean.TRUE.equals(success)) {
            return true;
        }
        return this.code != null && this.code == 200;
    }
}
