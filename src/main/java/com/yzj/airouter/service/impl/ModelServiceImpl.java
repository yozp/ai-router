package com.yzj.airouter.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yzj.airouter.model.entity.Model;
import com.yzj.airouter.mapper.ModelMapper;
import com.yzj.airouter.service.ModelService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/yozp">yunikon</a>
 */
@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model>  implements ModelService{

}
