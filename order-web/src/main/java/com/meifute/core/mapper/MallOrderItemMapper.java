package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.meifute.core.entity.MallOrderItem;
import com.meifute.core.entity.MallSkuSpec;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Created by liuliang on 2018/10/9.
 */
@Repository
public interface MallOrderItemMapper  extends BaseMapper<MallOrderItem> {


    Integer updateSplitCloudStockLog(@Param("newRelationId") String newRelationId, @Param("itemId")String itemId, @Param("oldRelationId")String oldRelationId);

    Integer updateSerialNumber(@Param("serialNumber") String serialNumber,  @Param("id") String id, @Param("securityNumber") String securityNumber);

    List<MallSkuSpec> getItemSpec();

    List<String> getAddressIdByName(@Param("userId") String userId, @Param("name") String name);

    List<String> getOrderItemBySkuAndOrderId(@Param("orderId") String orderId, @Param("skuCode") String skuCode);

    Integer updateSerialNumberByBatechId(@Param("serialNumber") String serialNumber, @Param("securityNumber") String securityNumber, @Param("ids") List<String> ids);

    List<MallOrderItem> getIdByNullSecurityNumberAndSerialNumber();
}
