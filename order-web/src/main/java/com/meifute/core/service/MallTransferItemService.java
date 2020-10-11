package com.meifute.core.service;

import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.codingapi.tx.annotation.TxTransaction;
import com.meifute.core.entity.MallTransferItem;
import com.meifute.core.mapper.MallTransferItemMapper;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @Auther: wxb
 * @Date: 2018/10/26 13:28
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class MallTransferItemService extends ServiceImpl<MallTransferItemMapper, MallTransferItem> {

    @Transactional
    @TxTransaction
    public boolean insertTransferItem(MallTransferItem entity) {
        if (ObjectUtils.isNullOrEmpty(entity.getId())) {
            entity.setId(IDUtils.genId());
        }
        entity.setCreateDate(new Date());
        log.info("====================MallTransferItem:{}", entity);
        return this.insert(entity);
    }
}
