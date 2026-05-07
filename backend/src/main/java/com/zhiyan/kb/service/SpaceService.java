package com.zhiyan.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpaceService {
    private final KbSpaceMapper mapper;

    public SpaceService(KbSpaceMapper mapper) {
        this.mapper = mapper;
    }

    public List<KbSpace> visibleSpaces() {
        return mapper.selectList(new LambdaQueryWrapper<KbSpace>().eq(KbSpace::getStatus, "NORMAL"));
    }
}
