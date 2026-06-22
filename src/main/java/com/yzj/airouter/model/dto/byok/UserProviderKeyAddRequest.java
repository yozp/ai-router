package com.yzj.airouter.model.dto.byok;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户提供者密钥添加请求
 */
@Data
public class UserProviderKeyAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提供者 ID
     */
    private Long providerId;

    /**
     * API Key
     */
    private String apiKey;
}
