package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.entity.ExpressCompany;
import com.meifute.core.mapper.ExpressCompanyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Classname ExpressCompanyService
 * @Description TODO
 * @Date 2020-02-17 11:32
 * @Created by MR. Xb.Wu
 */
@Slf4j
@Service
public class ExpressCompanyService extends ServiceImpl<ExpressCompanyMapper, ExpressCompany> {

    public String getHighestExpressCode(List<String> codeList) {
        List<ExpressCompany> companies = this.selectList(new EntityWrapper<ExpressCompany>()
                .in("code", codeList)
                .eq("is_del", "0"));
        //升序
        List<ExpressCompany> companyList = companies.stream().sorted(Comparator.comparing(ExpressCompany::getSort)).collect(Collectors.toList());
        return companyList.get(0).getCode();
    }

    public List<ExpressCompany> getExpressCompanyList() {
        List<ExpressCompany> companies = this.selectList(new EntityWrapper<ExpressCompany>()
                .eq("is_del", "0"));
        return companies;
    }
}
