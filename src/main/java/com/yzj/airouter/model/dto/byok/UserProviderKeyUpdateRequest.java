package com.yzj.airouter.model.dto.byok;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户提供者密钥更新请求
 */
@Data
public class UserProviderKeyUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 状态：active/inactive
     */
    private String status;
}
