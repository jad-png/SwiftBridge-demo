package com.swiftbridge.converter.utils;

public class Pacs008Xpaths {

    public static final String REFERENCE_INSTR_ID = "//doc:CstmrCdtTrfInitn/doc:PmtInf/doc:CdtTrfTxInf/doc:PmtId/doc:InstrId";
    public static final String REFERENCE_END_TO_END_ID = "//doc:CdtTrfTxInf/doc:PmtId/doc:EndToEndId";
    public static final String UETR = "//doc:CdtTrfTxInf/doc:PmtId/doc:UETR";

    public static final String AMOUNT_VALUE_INTERBANK = "//doc:CdtTrfTxInf/doc:IntrBkSttlmAmt";
    public static final String AMOUNT_VALUE_INSTD = "//doc:CdtTrfTxInf/doc:InstdAmt";
    public static final String AMOUNT_CCY_INTERBANK = "//doc:CdtTrfTxInf/doc:IntrBkSttlmAmt/@Ccy";
    public static final String AMOUNT_CCY_INSTD = "//doc:CdtTrfTxInf/doc:InstdAmt/@Ccy";

    public static final String SETTLEMENT_DATE_INTERBANK = "//doc:CdtTrfTxInf/doc:IntrBkSttlmDt";
    public static final String SETTLEMENT_DATE_REQUESTED = "//doc:CdtTrfTxInf/doc:ReqdExctnDt";

    public static final String DEBTOR_NAME = "//doc:CstmrCdtTrfInitn/doc:PmtInf/doc:Dbtr/doc:Nm";
    public static final String DEBTOR_NAME_FALLBACK = "//doc:CdtTrfTxInf/doc:InitgPty/doc:Nm";
    public static final String DBTR_BICFI = "//doc:CdtTrfTxInf/doc:DbtrAgt/doc:FinInstnId/doc:BICFI";
    public static final String DBTR_BIC = "//doc:CdtTrfTxInf/doc:DbtrAgt/doc:FinInstnId/doc:Cd";

    public static final String CREDITOR_NAME = "//doc:CstmrCdtTrfInitn/doc:PmtInf/doc:CdtTrfTxInf/doc:Cdtr/doc:Nm";
    public static final String CDTR_BICFI = "//doc:CdtTrfTxInf/doc:CdtrAgt/doc:FinInstnId/doc:BICFI";
    public static final String CDTR_BIC = "//doc:CdtTrfTxInf/doc:CdtrAgt/doc:FinInstnId/doc:Cd";

    public static final String CHARGE_BEARER_PMTINF = "//doc:PmtInf/doc:ChrgBr";
    public static final String CHARGE_BEARER_TX = "//doc:CdtTrfTxInf/doc:ChrgBr";

    public static final String DEBTOR_ADDRESS_ROOT = "//doc:CdtTrfTxInf/doc:Dbtr/doc:PstlAdr";
    public static final String CREDITOR_ADDRESS_ROOT = "//doc:CdtTrfTxInf/doc:Cdtr/doc:PstlAdr";

    private Pacs008Xpaths() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}
