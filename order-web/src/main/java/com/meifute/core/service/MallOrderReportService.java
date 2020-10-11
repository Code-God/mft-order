package com.meifute.core.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.dto.PageDto;
import com.meifute.core.dto.report.AgentTotalAmountSortRequest;
import com.meifute.core.dto.report.AgentTotalAmountSortResponseDTO;
import com.meifute.core.dto.report.BaseSortRequest;
import com.meifute.core.dto.report.NewAgent.NewGeneralAgentSortResponseDTO;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mapper.MallOrderReportMapper;
import com.meifute.core.mmall.common.utils.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @Auther: wang
 * @Description: 订单相关报表
 */
@Service
@Slf4j
public class MallOrderReportService extends ServiceImpl<MallOrderInfoMapper, MallOrderInfo> {


    @Autowired
    private MallOrderReportMapper mallOrderReportMapper;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private AgentFeign agentFeign;


    /**
     * 代理进出货排名
     *
     * @param request
     * @return
     */
    public PageDto<AgentTotalAmountSortResponseDTO> sortAgentTotalAmount(AgentTotalAmountSortRequest request, Page page) {
        //进货：统计所有总代的进货量，进货仅统计订单类型为【入云（直购）】【直发】，且订单状态为已完成的，不包含脚套和指套
        List<AgentTotalAmountSortResponseDTO> stockResult = mallOrderReportMapper.sortAgentTotalStockAmount(request);
        //出货：统计所有代理的出货量，统计【直发】+【提货】订单状态是已完成的，不包含脚套和指套
        List<AgentTotalAmountSortResponseDTO> shipmentResult = mallOrderReportMapper.sortAgentTotalShipmentAmount(request);
        PageDto pageResult = new PageDto();
        if (CollectionUtils.isEmpty(stockResult)) {
            if (!CollectionUtils.isEmpty(shipmentResult))
                pageResult = pageHelper(shipmentResult, page, pageResult);
            return pageResult;
        }
        if (CollectionUtils.isEmpty(shipmentResult)) {
            if (!CollectionUtils.isEmpty(stockResult))
                pageResult = pageHelper(stockResult, page, pageResult);
            return pageResult;
        }
        if (!CollectionUtils.isEmpty(shipmentResult) && !CollectionUtils.isEmpty(stockResult)) {
            // 判断根据进货还是出货量排名
            List<AgentTotalAmountSortResponseDTO> result = new ArrayList<>();
            if (request.getIsSortByStock()) {
                //进货排名
                result = sortByStockOrShipment(stockResult, shipmentResult, request.getIsSortByStock());

            } else {
                result = sortByStockOrShipment(shipmentResult, stockResult, request.getIsSortByStock());
            }

            return pageHelper(result, page, pageResult);
        }

        return pageResult;
    }

    /**
     * 代理进出货前三名
     *
     * @param request
     * @return
     */
    public Map<String, List<AgentTotalAmountSortResponseDTO>> sortAgentTotalAmountFirstThree(AgentTotalAmountSortRequest request) {
        Map<String, List<AgentTotalAmountSortResponseDTO>> map = new HashMap<>();
        List<AgentTotalAmountSortResponseDTO> stock = new ArrayList<>();
        List<AgentTotalAmountSortResponseDTO> shipment = new ArrayList<>();
        //进货：统计所有总代的进货量，进货仅统计订单类型为【入云（直购）】【直发】，且订单状态为已完成的，不包含脚套和指套
        List<AgentTotalAmountSortResponseDTO> stockResult = mallOrderReportMapper.sortAgentTotalStockAmount(request);
        //出货：统计所有代理的出货量，统计【直发】+【提货】订单状态是已完成的，不包含脚套和指套
        List<AgentTotalAmountSortResponseDTO> shipmentResult = mallOrderReportMapper.sortAgentTotalShipmentAmount(request);

        if (!CollectionUtils.isEmpty(stockResult)) {
            for (AgentTotalAmountSortResponseDTO dto : stockResult) {
                if ("1".equals(dto.getStockSort())) {
                    stock.add(dto);
                } else if ("2".equals(dto.getStockSort())) {
                    stock.add(dto);
                } else if ("3".equals(dto.getStockSort())) {
                    stock.add(dto);
                }
            }
        }

        if (!CollectionUtils.isEmpty(shipmentResult)) {
            for (AgentTotalAmountSortResponseDTO dto : shipmentResult) {
                if ("1".equals(dto.getShipmentSort())) {
                    shipment.add(dto);
                } else if ("2".equals(dto.getShipmentSort())) {
                    shipment.add(dto);
                } else if ("3".equals(dto.getShipmentSort())) {
                    shipment.add(dto);
                }
            }
        }

        map.put("stock",stock);
        map.put("shipment",shipment);
        return map;
    }

    /**
     * 代理进出货排名报表导出
     *
     * @param response
     * @return
     * @throws Exception
     */
    public String exportAgentTotalAmount(AgentTotalAmountSortRequest request, HttpServletResponse response) throws Exception {
        List<AgentTotalAmountSortResponseDTO> result = this.stockAndShipment(request);

        if (!CollectionUtils.isEmpty(result)) {
            ExcelUtil excelUtil = new ExcelUtil();
            LinkedHashMap<String, String> titleMap = new LinkedHashMap<>();
            titleMap.put("代理姓名", "name");
            titleMap.put("手机号", "phone");
            titleMap.put("进货量", "stockSkuAmount");
            titleMap.put("进货排名", "stockSort");
            titleMap.put("出货量", "shipmentSkuAmount");
            titleMap.put("出货排名", "shipmentSort");
            excelUtil.buildExcel(titleMap, result, ExcelUtil.DEFAULT_ROW_MAX, response);
            return "success";
        } else {
            return "false";
        }
    }

    private List<AgentTotalAmountSortResponseDTO> stockAndShipment(AgentTotalAmountSortRequest request) {
        List<AgentTotalAmountSortResponseDTO> stockResult = mallOrderReportMapper.sortAgentTotalStockAmount(request);
        List<AgentTotalAmountSortResponseDTO> shipmentResult = mallOrderReportMapper.sortAgentTotalShipmentAmount(request);
        if (CollectionUtils.isEmpty(stockResult)) {
            return shipmentResult;
        }
        if (CollectionUtils.isEmpty(shipmentResult)) {
            return stockResult;
        }
        List<AgentTotalAmountSortResponseDTO> result = new ArrayList<>();
        if (!CollectionUtils.isEmpty(shipmentResult) && !CollectionUtils.isEmpty(stockResult)) {
            // 判断根据进货还是出货量排名
            if (request.getIsSortByStock()) {
                //进货排名
                result = sortByStockOrShipment(stockResult, shipmentResult, request.getIsSortByStock());

            } else {
                result = sortByStockOrShipment(shipmentResult, stockResult, request.getIsSortByStock());
            }
        }
        return result;
    }

    private PageDto pageHelper(List<AgentTotalAmountSortResponseDTO> result, Page page, PageDto pageResult) {
        List<List<AgentTotalAmountSortResponseDTO>> partition = ListUtils.partition(result, page.getSize());
        List<AgentTotalAmountSortResponseDTO> agentTotalAmountSortResponseDTOS = partition.get(page.getCurrent() - 1);
        pageResult.setRecords(agentTotalAmountSortResponseDTOS);
        if (!CollectionUtils.isEmpty(result))
            pageResult.setTotal(result.size());
        return pageResult;
    }

    private List<AgentTotalAmountSortResponseDTO> sortByStockOrShipment
            (List<AgentTotalAmountSortResponseDTO> list1, List<AgentTotalAmountSortResponseDTO> list2, Boolean
                    isSortByStock) {
        List<String> list = new ArrayList<>();
        for (AgentTotalAmountSortResponseDTO dto1 : list1) {
            list.add(dto1.getUserId());
            for (AgentTotalAmountSortResponseDTO dto2 : list2) {
                if (dto1.getUserId().equals(dto2.getUserId())) {
                    if (isSortByStock) {
                        dto1.setShipmentSkuAmount(dto2.getShipmentSkuAmount());
                        dto1.setShipmentSort(dto2.getShipmentSort());
                    } else {
                        dto1.setStockSkuAmount(dto2.getStockSkuAmount());
                        dto1.setStockSort(dto2.getStockSort());
                    }
                }
            }
        }

        for (AgentTotalAmountSortResponseDTO dto2 : list2) {
            if (!list.contains(dto2.getUserId())) {
                list1.add(dto2);
            }
        }
        return list1;
    }

    /**
     * 代新增总代排名
     *
     * @param request
     * @return
     */
    public PageDto<NewGeneralAgentSortResponseDTO> sortNewGeneralAgent(BaseSortRequest request, Page page) {
        List<NewGeneralAgentSortResponseDTO> newGeneralAgentSortResponseDTOS = mallOrderReportMapper.sortNewGeneralAgent(request, page);
        return resultPageDto(newGeneralAgentSortResponseDTOS, page);
    }

    /**
     * 代新增总代排名报表导出
     *
     * @param response
     * @return
     * @throws Exception
     */
    public String exportNewGeneralAgent(BaseSortRequest request, HttpServletResponse response) throws Exception {
        List<NewGeneralAgentSortResponseDTO> newGeneralAgentSortResponseDTOS = mallOrderReportMapper.sortNewGeneralAgentReport(request);

        if (!CollectionUtils.isEmpty(newGeneralAgentSortResponseDTOS)) {
            ExcelUtil excelUtil = new ExcelUtil();
            LinkedHashMap<String, String> titleMap = new LinkedHashMap<>();
            titleMap.put("排名", "sort");
            titleMap.put("姓名", "name");
            titleMap.put("手机号", "phone");
            titleMap.put("新增总代数", "agentAmount");
            excelUtil.buildExcel(titleMap, newGeneralAgentSortResponseDTOS, ExcelUtil.DEFAULT_ROW_MAX, response);
            return "success";
        } else {
            return "false";
        }
    }

    /**
     * 新增总代前三名
     *
     * @param request
     * @return
     */
    public List<NewGeneralAgentSortResponseDTO> sortNewGeneralAgentFirstThree(BaseSortRequest request) {
        List<NewGeneralAgentSortResponseDTO> result = new ArrayList<>();
        List<NewGeneralAgentSortResponseDTO> newGeneralAgentSortResponseDTOS = mallOrderReportMapper.sortNewGeneralAgentFirstThree(request);
        if (!CollectionUtils.isEmpty(newGeneralAgentSortResponseDTOS)) {
            for (NewGeneralAgentSortResponseDTO dto : newGeneralAgentSortResponseDTOS) {
                if ("1".equals(dto.getSort())) {
                    result.add(dto);
                } else if ("2".equals(dto.getSort())) {
                    result.add(dto);
                } else if ("3".equals(dto.getSort())) {
                    result.add(dto);
                }
            }
        }
        return result;
    }

    private PageDto resultPageDto(List list, Page page) {
        PageDto pageResult = new PageDto();
        pageResult.setRecords(list);
        if (!CollectionUtils.isEmpty(list))
            pageResult.setTotal(page.getTotal());
        return pageResult;
    }



}
