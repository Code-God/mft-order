package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.meifute.core.dto.report.AgentTotalAmountSortRequest;
import com.meifute.core.dto.report.AgentTotalAmountSortResponseDTO;
import com.meifute.core.dto.report.BaseSortRequest;
import com.meifute.core.dto.report.NewAgent.NewGeneralAgentSortResponseDTO;
import com.meifute.core.entity.MallOrderInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * wang
 */
@Repository
public interface MallOrderReportMapper extends BaseMapper<MallOrderInfo> {

    /**
     * 代理进货总量排名
     * @param agentTotalAmountSortRequest
     * @return
     */
    List<AgentTotalAmountSortResponseDTO> sortAgentTotalStockAmount(@Param("param") AgentTotalAmountSortRequest agentTotalAmountSortRequest);

    /**
     * 代理出货总量排名
     * @param agentTotalAmountSortRequest
     * @return
     */
    List<AgentTotalAmountSortResponseDTO> sortAgentTotalShipmentAmount(@Param("param") AgentTotalAmountSortRequest agentTotalAmountSortRequest);

    /**
     * 新增总代排名
     * @return
     */
    List<NewGeneralAgentSortResponseDTO> sortNewGeneralAgent(@Param("param") BaseSortRequest baseSortRequest, Pagination page);

    /**
     * 新增总代排名导出
     * @return
     */
    List<NewGeneralAgentSortResponseDTO> sortNewGeneralAgentReport(@Param("param") BaseSortRequest baseSortRequest);
/**
     * 新增总代排名前三名
     * @return
     */
    List<NewGeneralAgentSortResponseDTO> sortNewGeneralAgentFirstThree(@Param("param") BaseSortRequest baseSortRequest);


}
