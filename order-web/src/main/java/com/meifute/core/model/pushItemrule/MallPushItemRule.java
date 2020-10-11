package com.meifute.core.model.pushItemrule;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Classname MallPushItemRule
 * @Description TODO
 * @Date 2020-06-19 17:36
 * @Created by MR. Xb.Wu
 */
@TableName("mall_push_item_rule")
@Data
public class MallPushItemRule implements Serializable {

    @TableId
    private String id;

    @TableField("title")
    @ApiModelProperty("商品名称")
    private String title;

    @TableField("sku_code")
    @ApiModelProperty("商品SKU")
    private String skuCode;

    @TableField("replace_title")
    @ApiModelProperty("调整发货商品名称")
    private String replaceTitle;

    @TableField("replace_sku_code")
    @ApiModelProperty("调整发货商品SKU")
    private String replaceSkuCode;

    @TableField("proportion")
    @ApiModelProperty("发货比例: 例1:3 英文冒号")
    private String proportion;

    @TableField("valid_start_date")
    @ApiModelProperty("有效起始时间")
    private Date validStartDate;

    @TableField("valid_end_date")
    @ApiModelProperty("有效截止时间")
    private Date validEndDate;

    @TableField("online")
    @ApiModelProperty("0关闭/1开启")
    private String online;

    @TableField("create_date")
    @ApiModelProperty("创建时间")
    private Date createDate;

    @TableField("is_del")
    @ApiModelProperty("逻辑删除：0正常/1删除")
    private String isDel;
}
