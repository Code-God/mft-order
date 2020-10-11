package com.meifute.core.util;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.meifute.core.model.qimen.QiMenItem;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @auther liuliang
 * @date 2020/2/18 2:19 PM
 */
public class DeliveryOrderConfirmRequest {
    private DeliveryOrderConfirmRequest.DeliverOrder deliverOrder;
    @JacksonXmlElementWrapper(
            localName = "orderLines"
    )
    @JacksonXmlProperty(
            localName = "orderLine"
    )
    private List<DeliveryOrderConfirmRequest.OrderLine> orderLines = new ArrayList();


    @JacksonXmlElementWrapper(
            localName = "packages"
    )
    @JacksonXmlProperty(
            localName = "package"
    )
    private List<DeliveryOrderConfirmRequest.QiMenPackage> packages = new ArrayList<>();

    public String getApiMethod() {
        return "deliveryorder.confirm";
    }

    public DeliveryOrderConfirmRequest() {
    }

    public DeliveryOrderConfirmRequest.DeliverOrder getEntryOrder() {
        return this.deliverOrder;
    }

    public List<DeliveryOrderConfirmRequest.OrderLine> getOrderLines() {
        return this.orderLines;
    }

    public DeliverOrder getDeliverOrder() {
        return deliverOrder;
    }

    public void setDeliverOrder(DeliverOrder deliverOrder) {
        this.deliverOrder = deliverOrder;
    }

    public List<QiMenPackage> getPackages() {
        return packages;
    }

    public void setPackages(List<QiMenPackage> packages) {
        this.packages = packages;
    }

    public void setEntryOrder(DeliveryOrderConfirmRequest.DeliverOrder entryOrder) {
        this.deliverOrder = entryOrder;
    }

    public void setOrderLines(List<DeliveryOrderConfirmRequest.OrderLine> orderLines) {
        this.orderLines = orderLines;
    }


    @Override
    public int hashCode() {

        return Objects.hash(deliverOrder, orderLines);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DeliveryOrderConfirmRequest)) {
            return false;
        } else {
            DeliveryOrderConfirmRequest other = (DeliveryOrderConfirmRequest)o;
            if (!other.canEqual(this)) {
                return false;
            } else if (!super.equals(o)) {
                return false;
            } else {
                Object this$entryOrder = this.getEntryOrder();
                Object other$entryOrder = other.getEntryOrder();
                if (this$entryOrder == null) {
                    if (other$entryOrder != null) {
                        return false;
                    }
                } else if (!this$entryOrder.equals(other$entryOrder)) {
                    return false;
                }

                Object this$orderLines = this.getOrderLines();
                Object other$orderLines = other.getOrderLines();
                if (this$orderLines == null) {
                    if (other$orderLines != null) {
                        return false;
                    }
                } else if (!this$orderLines.equals(other$orderLines)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof DeliveryOrderConfirmRequest;
    }


    @Override
    public String toString() {
        return "DeliveryOrderConfirmRequest{" +
                "deliverOrder=" + deliverOrder +
                ", orderLines=" + orderLines +
                ", packages=" + packages +
                '}';
    }

    @Data
    public static class QiMenPackage implements Serializable {

        private String expressCode;

        private String invoiceNo;

        private String logisticsCode;

        private String packageCode;

        @JacksonXmlElementWrapper(
                localName = "items"
        )
        @JacksonXmlProperty(
                localName = "item"
        )
        private List<QiMenItem> items;
    }

    public static class OrderLine {
        private String outBizCode;
        private String orderLineNo;
        private String ownerCode;
        private String itemCode;
        private String itemId;
        private String itemName;
        private String inventoryType;
        private int planQty;
        private int actualQty;
        private String batchCode;
        private LocalDate productDate;
        private LocalDate expireDate;
        private String produceCode;
        private String remark;

        public OrderLine() {
        }

        public String getOutBizCode() {
            return this.outBizCode;
        }

        public String getOrderLineNo() {
            return this.orderLineNo;
        }

        public String getOwnerCode() {
            return this.ownerCode;
        }

        public String getItemCode() {
            return this.itemCode;
        }

        public String getItemId() {
            return this.itemId;
        }

        public String getItemName() {
            return this.itemName;
        }

        public String getInventoryType() {
            return this.inventoryType;
        }

        public int getPlanQty() {
            return this.planQty;
        }

        public int getActualQty() {
            return this.actualQty;
        }

        public String getBatchCode() {
            return this.batchCode;
        }

        public LocalDate getProductDate() {
            return this.productDate;
        }

        public LocalDate getExpireDate() {
            return this.expireDate;
        }

        public String getProduceCode() {
            return this.produceCode;
        }

        public String getRemark() {
            return this.remark;
        }

        public void setOutBizCode(String outBizCode) {
            this.outBizCode = outBizCode;
        }

        public void setOrderLineNo(String orderLineNo) {
            this.orderLineNo = orderLineNo;
        }

        public void setOwnerCode(String ownerCode) {
            this.ownerCode = ownerCode;
        }

        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public void setInventoryType(String inventoryType) {
            this.inventoryType = inventoryType;
        }

        public void setPlanQty(int planQty) {
            this.planQty = planQty;
        }

        public void setActualQty(int actualQty) {
            this.actualQty = actualQty;
        }

        public void setBatchCode(String batchCode) {
            this.batchCode = batchCode;
        }

        public void setProductDate(LocalDate productDate) {
            this.productDate = productDate;
        }

        public void setExpireDate(LocalDate expireDate) {
            this.expireDate = expireDate;
        }

        public void setProduceCode(String produceCode) {
            this.produceCode = produceCode;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }


        protected boolean canEqual(Object other) {
            return other instanceof DeliveryOrderConfirmRequest.OrderLine;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderLine orderLine = (OrderLine) o;
            return planQty == orderLine.planQty &&
                    actualQty == orderLine.actualQty &&
                    Objects.equals(outBizCode, orderLine.outBizCode) &&
                    Objects.equals(orderLineNo, orderLine.orderLineNo) &&
                    Objects.equals(ownerCode, orderLine.ownerCode) &&
                    Objects.equals(itemCode, orderLine.itemCode) &&
                    Objects.equals(itemId, orderLine.itemId) &&
                    Objects.equals(itemName, orderLine.itemName) &&
                    Objects.equals(inventoryType, orderLine.inventoryType) &&
                    Objects.equals(batchCode, orderLine.batchCode) &&
                    Objects.equals(productDate, orderLine.productDate) &&
                    Objects.equals(expireDate, orderLine.expireDate) &&
                    Objects.equals(produceCode, orderLine.produceCode) &&
                    Objects.equals(remark, orderLine.remark);
        }

        @Override
        public int hashCode() {
            return Objects.hash(outBizCode, orderLineNo, ownerCode, itemCode, itemId, itemName, inventoryType, planQty, actualQty, batchCode, productDate, expireDate, produceCode, remark);
        }

        @Override
        public String toString() {
            return "OrderLine{" +
                    "outBizCode='" + outBizCode + '\'' +
                    ", orderLineNo='" + orderLineNo + '\'' +
                    ", ownerCode='" + ownerCode + '\'' +
                    ", itemCode='" + itemCode + '\'' +
                    ", itemId='" + itemId + '\'' +
                    ", itemName='" + itemName + '\'' +
                    ", inventoryType='" + inventoryType + '\'' +
                    ", planQty=" + planQty +
                    ", actualQty=" + actualQty +
                    ", batchCode='" + batchCode + '\'' +
                    ", productDate=" + productDate +
                    ", expireDate=" + expireDate +
                    ", produceCode='" + produceCode + '\'' +
                    ", remark='" + remark + '\'' +
                    '}';
        }
    }

    public static class DeliverOrder {
        private int totalOrderLines;
        private String entryOrderCode;
        private String ownerCode;
        private String warehouseCode;
        private String entryOrderId;
        private String entryOrderType;
        private String outBizCode;
        private String confirmType;
        private String status;
        private double freight;
        private LocalDateTime operateTime;
        private String remark;

        public DeliverOrder() {
        }

        public int getTotalOrderLines() {
            return this.totalOrderLines;
        }

        public String getEntryOrderCode() {
            return this.entryOrderCode;
        }

        public String getOwnerCode() {
            return this.ownerCode;
        }

        public String getWarehouseCode() {
            return this.warehouseCode;
        }

        public String getEntryOrderId() {
            return this.entryOrderId;
        }

        public String getEntryOrderType() {
            return this.entryOrderType;
        }

        public String getOutBizCode() {
            return this.outBizCode;
        }

        public String getConfirmType() {
            return this.confirmType;
        }

        public String getStatus() {
            return this.status;
        }

        public double getFreight() {
            return this.freight;
        }

        public LocalDateTime getOperateTime() {
            return this.operateTime;
        }

        public String getRemark() {
            return this.remark;
        }

        public void setTotalOrderLines(int totalOrderLines) {
            this.totalOrderLines = totalOrderLines;
        }

        public void setEntryOrderCode(String entryOrderCode) {
            this.entryOrderCode = entryOrderCode;
        }

        public void setOwnerCode(String ownerCode) {
            this.ownerCode = ownerCode;
        }

        public void setWarehouseCode(String warehouseCode) {
            this.warehouseCode = warehouseCode;
        }

        public void setEntryOrderId(String entryOrderId) {
            this.entryOrderId = entryOrderId;
        }

        public void setEntryOrderType(String entryOrderType) {
            this.entryOrderType = entryOrderType;
        }

        public void setOutBizCode(String outBizCode) {
            this.outBizCode = outBizCode;
        }

        public void setConfirmType(String confirmType) {
            this.confirmType = confirmType;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setFreight(double freight) {
            this.freight = freight;
        }

        public void setOperateTime(LocalDateTime operateTime) {
            this.operateTime = operateTime;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }


        protected boolean canEqual(Object other) {
            return other instanceof DeliveryOrderConfirmRequest.DeliverOrder;
        }


        @Override
        public String toString() {
            return "DeliverOrder{" +
                    "totalOrderLines=" + totalOrderLines +
                    ", entryOrderCode='" + entryOrderCode + '\'' +
                    ", ownerCode='" + ownerCode + '\'' +
                    ", warehouseCode='" + warehouseCode + '\'' +
                    ", entryOrderId='" + entryOrderId + '\'' +
                    ", entryOrderType='" + entryOrderType + '\'' +
                    ", outBizCode='" + outBizCode + '\'' +
                    ", confirmType='" + confirmType + '\'' +
                    ", status='" + status + '\'' +
                    ", freight=" + freight +
                    ", operateTime=" + operateTime +
                    ", remark='" + remark + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeliverOrder that = (DeliverOrder) o;
            return totalOrderLines == that.totalOrderLines &&
                    Double.compare(that.freight, freight) == 0 &&
                    Objects.equals(entryOrderCode, that.entryOrderCode) &&
                    Objects.equals(ownerCode, that.ownerCode) &&
                    Objects.equals(warehouseCode, that.warehouseCode) &&
                    Objects.equals(entryOrderId, that.entryOrderId) &&
                    Objects.equals(entryOrderType, that.entryOrderType) &&
                    Objects.equals(outBizCode, that.outBizCode) &&
                    Objects.equals(confirmType, that.confirmType) &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(operateTime, that.operateTime) &&
                    Objects.equals(remark, that.remark);
        }

        @Override
        public int hashCode() {

            return Objects.hash(totalOrderLines, entryOrderCode, ownerCode, warehouseCode, entryOrderId, entryOrderType, outBizCode, confirmType, status, freight, operateTime, remark);
        }
    }
}
