package com.cxf.batishelper.repo;

import com.cxf.batishelper.model.entity.UserDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author cxf
 * @since 2020/08/11
 */
@Repository
@Slf4j
public class UserRepository {
    @Autowired
    private UserMapper mapper;

    public UserDO selectById(Long id) {
        return mapper.selectById(id);
    }

    public void update(Long id, UserDO entity) {
        entity.setId(id);
        mapper.updateById(entity);
    }

    public Long insert(UserDO entity) {
        mapper.insert(entity);
        return entity.getId();
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
