package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.codingapi.tx.annotation.TxTransaction;
import com.meifute.core.dto.MallSkuDto;
import com.meifute.core.dto.PageDto;
import com.meifute.core.dto.RegulateDto;
import com.meifute.core.entity.*;
import com.meifute.core.feignclient.AdminFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.MallRegulateGoodsMapper;
import com.meifute.core.mapper.MallRegulateInfoMapper;
import com.meifute.core.mmall.common.check.MallPreconditions;
import com.meifute.core.mmall.common.dto.BeanMapper;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.mmall.common.utils.StringUtils;
import com.meifute.core.util.MybatisPageUtil;
import com.meifute.core.util.UserUtils;
import com.meifute.core.vo.GetRegulateParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @Auther: wxb
 * @Date: 2018/10/26 13:21
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Slf4j
@Service
public class MallRegulateInfoService extends ServiceImpl<MallRegulateInfoMapper, MallRegulateInfo> {
    @Autowired
    private AdminFeign adminFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private MallRegulateInfoMapper mallRegulateInfoMapper;
    @Autowired
    private MallRegulateGoodsMapper mallRegulateGoodsMapper;

    @Transactional
    @TxTransaction
    public boolean insertRegulateInfo(MallRegulateInfo entity) {
        if (ObjectUtils.isNullOrEmpty(entity.getId())) {
            entity.setId(IDUtils.genId());
        }
        entity.setCreateDate(new Date());
        return this.insert(entity);
    }


    /**
     * 调剂单列表
     *
     * @param param
     * @return
     */
    public PageDto<RegulateDto> queryMallRegulatePageList(@RequestBody GetRegulateParam param) {
        MallRegulateInfo mallRegulateInfo = new MallRegulateInfo();
        BeanMapper.copy(param, mallRegulateInfo);
        mallRegulateInfo.setPageCurrent(param.getPageCurrent());
        mallRegulateInfo.setPageSize(param.getPageSize());
        List<Admin> adminList = null;
       /* if (!StringUtils.isEmpty(param.getAdminName())) {
            adminList = adminFeign.selectAdminInfoByName(param.getAdminName());
        }*/
        List<MallUser> userList = null;
        if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getName(), param.getPhone(), param.getNickName()))) {
            userList = userFeign.getUserByInput(param.getNickName(), param.getName(), param.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userList)) {
                return null;
            }
        }
      /*  List<Long> adminIdList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(adminIdList)) {
            for (Admin admin : adminList) {
                adminIdList.add(admin.getId());
            }
            mallRegulateInfo.setAdminIdList(adminIdList);
        }*/
        List<String> userIdList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userList)) {
            for (MallUser malluser : userList) {
                userIdList.add(malluser.getId());
            }
            mallRegulateInfo.setMallUserIdList(userIdList);
        }


        Page page = MybatisPageUtil.getPage(mallRegulateInfo.getPageCurrent(), mallRegulateInfo.getPageSize());
        List<Admin> admins = new ArrayList<>();
        if (!StringUtils.isEmpty(mallRegulateInfo.getAdminName())) {
            admins = adminFeign.selectAdminInfoByName(mallRegulateInfo.getAdminName());
            if (!CollectionUtils.isEmpty(admins)) {
                mallRegulateInfo.setAdminIdList(admins.stream().map(Admin::getId).collect(Collectors.toList()));
            }
        } else {
            admins = adminFeign.selectAdminInfoByName("");
        }
        log.info("admins:{}", admins);
        List<MallRegulateInfo> mallRegulateInfoPage = mallRegulateInfoMapper.queryMallRegulatePageList(mallRegulateInfo, page);
        List<RegulateDto> result = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mallRegulateInfoPage)) {
            for (MallRegulateInfo record : mallRegulateInfoPage) {
                RegulateDto dto = new RegulateDto();
                BeanMapper.copy(record, dto);
                //用户
                dto.setMallUser(UserUtils.getUserInfoByCacheOrId(record.getMallUserId()));
                if (StringUtils.isNotBlank(record.getAdminId())) {
                    log.info("record.getAdminId():{}", record.getAdminId());
                    Admin admin = new Admin();
                    if (!CollectionUtils.isEmpty(admins)) {
                        admins.forEach(a -> {
                            if (record.getAdminId().equals(a.getId()))
                                admin.setName(a.getName());
                        });
                    }
                    dto.setAdmin(admin);
                }
                //商品
                List<MallRegulateGoods> goods = mallRegulateGoodsMapper.selectList(new EntityWrapper<MallRegulateGoods>()
                        .eq("regulate_id", record.getId())
                        .eq("is_del", "0"));
                List<MallSkuDto> skuDtos = new ArrayList<>();
                if (goods.size() > 0) {
                    for (MallRegulateGoods item : goods) {
                        MallSkuDto mallSkuDto = new MallSkuDto();
                        mallSkuDto.setAmount(item.getAmount().intValue());
                        MallSku mallSku = new MallSku();
                        mallSku.setItemId(item.getItemId());
                        mallSku.setSkuCode(item.getSkuCode());
                        MallSku sku = itemFeign.getSkuByParam(mallSku);
                        mallSkuDto.setMallSku(sku);
                        skuDtos.add(mallSkuDto);
                    }
                }
                dto.setSkuDtos(skuDtos);
                result.add(dto);
            }
        }
        PageDto pageResult = new PageDto();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(result);
        return pageResult;
    }

    public List<MallRegulateInfo> queryRegulationInfoListByParam(MallRegulateInfo param) {
        List<MallRegulateInfo> regulateInfoList = mallRegulateInfoMapper.selectList(new EntityWrapper<MallRegulateInfo>()
                .eq("is_del", "0")
                .eq(!StringUtils.isEmpty(param.getMallUserId()), "mall_user_id", param.getMallUserId())
                .eq(!StringUtils.isEmpty(param.getRegulateType()), "regulate_type", param.getRegulateType()) // 商务云调剂-云库存增加
                .orderBy("create_date", true)
        );
        return regulateInfoList;
    }
}
