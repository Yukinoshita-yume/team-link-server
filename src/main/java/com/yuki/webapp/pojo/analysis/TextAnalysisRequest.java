package com.yuki.webapp.pojo.analysis;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * {@link com.yuki.webapp.contoller.TextAnalysisController} 的请求体。
 */
@Data
public class TextAnalysisRequest {
    @NotBlank(message = "输入文本不能为空")
    private String text;
}
