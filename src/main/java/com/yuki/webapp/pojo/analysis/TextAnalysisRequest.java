package com.yuki.webapp.pojo.analysis;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TextAnalysisRequest {
    @NotBlank(message = "输入文本不能为空")
    private String text;
}
