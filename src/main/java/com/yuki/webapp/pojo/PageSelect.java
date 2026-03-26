package com.yuki.webapp.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageSelect {

    private Integer page = 1;
    private Integer pageSize = 5;
    private String content;
}
