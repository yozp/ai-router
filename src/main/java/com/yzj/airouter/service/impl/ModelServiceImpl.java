package com.yzj.airouter.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yzj.airouter.model.dto.model.ModelQueryRequest;
import com.yzj.airouter.model.entity.Model;
import com.yzj.airouter.mapper.ModelMapper;
import com.yzj.airouter.model.vo.ModelVO;
import com.yzj.airouter.service.ModelService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/yozp">yunikon</a>
 */
@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model>  implements ModelService{

    @Override
    public QueryWrapper getQueryWrapper(ModelQueryRequest modelQueryRequest) {
        return null;
    }

    @Override
    public ModelVO getModelVO(Model model) {
        return null;
    }

    @Override
    public List<ModelVO> getModelVOList(List<Model> modelList) {
        return null;
    }

    @Override
    public Model getByModelKey(String modelKey) {
        return null;
    }

    @Override
    public List<Model> getActiveModels() {
        return null;
    }

    @Override
    public List<Model> getActiveModelsByProviderId(Long providerId) {
        return null;
    }

    @Override
    public List<Model> getActiveModelsByType(String modelType) {
        return null;
    }

    @Override
    public void updateModelMetrics(Long modelId, String healthStatus, Integer avgLatency, BigDecimal successRate, BigDecimal score) {

    }
}
