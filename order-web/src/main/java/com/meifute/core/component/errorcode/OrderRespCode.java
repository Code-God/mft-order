package com.meifute.core.component.errorcode;

/**
 * @Auther: wxb
 * @Date: 2018/9/26 14:16
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
public interface OrderRespCode {

    String ZORE_NOT_ORDER = "020001";
    String REPEAT_ORDER = "020002";
    String ORDINARY_USER_NOTTOBUY = "020003";
    String PAY_VOUCHER_ISNOT = "020004";
    String INVALID_ADDRESS = "020005";
    String DONT_DELIVER_GOODS = "020006";
    String LACK_OF_STOCK = "020007";
    String DONT_HAVE_ORDER = "020008";
    String ADD_ORDER_FAIL = "020009";
    String IN_IS_NOT = "020010";
    String OUT_IS_NOT = "020011";
    String STOCK_NOT_FOUND = "020012";
    String CAN_NOT_VERIFY_ORDER = "020013";
    String ORDER_AREADY_VERIFY ="020014";
    String IS_NOT_YOUR_AGENT ="020015";
    String IS_NOT_HAVE_DATA ="020016";
    String IS_NOT_TO_CANCEL = "020017";
    String IS_NOT_TO_RECEVIED = "020018";
    String NOT_TO_VERIFY = "020019";
    String IS_CALCEL = "020020";
    String TRANSPORTGOODS_IS_NOT_NULL = "020021";
    String PARAM_NOT_FOUND="020023";
    String NOT_AGENT="020024";
    String NOT_SEARCH_ORDER="020025";
    String ORDER_AREAY_END ="020026";
    String ACCOUNT_NOT_ENOUGH="020027";
    String AGENT_LEVEL_NOT_FOUR="020031";

}
