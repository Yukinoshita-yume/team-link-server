package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.pojo.analysis.TextAnalysisRequest;
import com.yuki.webapp.pojo.analysis.TextAnalysisResult;
import com.yuki.webapp.service.TextAnalysisService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analysis")
public class TextAnalysisController {

    @Autowired
    private TextAnalysisService textAnalysisService;

    /**
     * 文本能力分析入口：
     * 1) 文本预处理 + 实体识别
     * 2) LLM 结构化画像
     * 3) 五维评分
     */
    @PostMapping("/text-profile")
    public Result<TextAnalysisResult> analyze(@Valid @RequestBody TextAnalysisRequest request) {
        TextAnalysisResult result = textAnalysisService.analyze(request.getText());
        return Result.success(result);
    }
}
