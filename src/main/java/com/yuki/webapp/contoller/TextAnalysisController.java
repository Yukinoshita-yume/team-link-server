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

/**
 * 文本分析 REST 接口，与竞赛检索等业务解耦。
 */
@RestController
@RequestMapping("/analysis")
public class TextAnalysisController {

    @Autowired
    private TextAnalysisService textAnalysisService;

    /**
     * 文本能力分析入口：
     * <ol>
     *   <li>文本预处理与分段</li>
     *   <li>实体识别（LLM，失败则规则词典兜底）</li>
     *   <li>结构化画像（LLM，失败则规则模板兜底）</li>
     *   <li>五维评分、综合解释模板与置信度标记</li>
     * </ol>
     *
     * @param request 请求体，字段 {@code text} 为待分析正文
     * @return {@link Result} 包装 {@link TextAnalysisResult}
     */
    @PostMapping("/text-profile")
    public Result<TextAnalysisResult> analyze(@Valid @RequestBody TextAnalysisRequest request) {
        TextAnalysisResult result = textAnalysisService.analyze(request.getText());
        return Result.success(result);
    }
}
