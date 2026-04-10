package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.pojo.analysis.TextAnalysisResult;
import com.yuki.webapp.pojo.profile.CompetenceCardDTO;
import com.yuki.webapp.pojo.profile.SkillTagsUpdateRequest;
import com.yuki.webapp.service.UserProfileMySqlService;
import com.yuki.webapp.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户画像能力卡片：数据存于 Qdrant，供前端雷达图、标签、擅长方向、时间热力图等展示与编辑。
 */
@RestController
@RequestMapping("/user/profile")
public class UserProfileController {

    @Autowired
    private UserProfileMySqlService userProfileMySqlService;

    /**
     * 获取当前登录用户的能力卡片（无记录时返回各字段默认值）。
     */
    @GetMapping("/competence-card")
    public Result<CompetenceCardDTO> getCompetenceCard() {
        int userId = currentUserId();
        return Result.success(userProfileMySqlService.getOrGenerateCompetenceCard(userId));
    }

    /**
     * 全量保存能力卡片（覆盖 Qdrant 中该用户的 point payload 与向量）。
     */
    @PutMapping("/competence-card")
    public Result<Void> saveCompetenceCard(@RequestBody CompetenceCardDTO body) {
        return Result.error("暂不支持手动保存能力画像，请通过个人简介自动生成");
    }

    /**
     * 手动修正技能标签（仅更新标签列，其余画像字段保留）。
     */
    @PutMapping("/skill-tags")
    public Result<Void> updateSkillTags(@RequestBody SkillTagsUpdateRequest request) {
        return Result.error("暂不支持手动修改技能标签，请通过个人简介自动生成");
    }

    /**
     * 将文本分析结果一键写入能力卡片（覆盖同用户 Qdrant 点），便于与 {@code POST /analysis/text-profile} 串联。
     */
    @PostMapping("/apply-text-analysis")
    public Result<Void> applyTextAnalysis(@RequestBody TextAnalysisResult analysis) {
        int userId = currentUserId();
        userProfileMySqlService.saveFromAnalysis(userId, "manual_apply", analysis);
        return Result.success();
    }

    private static int currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Object id = claims.get("id");
        if (id instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalStateException("未登录或 token 中缺少用户 id");
    }
}
