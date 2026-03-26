package com.swiftbridge.converter.utils;

public class Pacs008Xpaths {

        public static final String REFERENCE_INSTR_ID = "//doc:PmtId/doc:InstrId";
        public static final String REFERENCE_END_TO_END_ID = "//doc:PmtId/doc:EndToEndId";
        public static final String UETR = "//doc:PmtId/doc:UETR";

        public static final String AMOUNT_VALUE_INTERBANK = "//doc:IntrBkSttlmAmt";
        public static final String AMOUNT_VALUE_INSTD = "//doc:InstdAmt";
        public static final String AMOUNT_CCY_INTERBANK = "//doc:IntrBkSttlmAmt/@Ccy";
        public static final String AMOUNT_CCY_INSTD = "//doc:InstdAmt/@Ccy";

        public static final String SETTLEMENT_DATE_INTERBANK = "//doc:IntrBkSttlmDt";
        public static final String SETTLEMENT_DATE_REQUESTED = "//doc:ReqdExctnDt";

        public static final String DEBTOR_NAME = "//doc:Dbtr/doc:Nm";
        public static final String DEBTOR_NAME_FALLBACK = "//doc:InitgPty/doc:Nm";
        public static final String INITIATING_PARTY_NAME = "//doc:InitgPty/doc:Nm";
        public static final String DBTR_BICFI = "//doc:DbtrAgt//doc:BICFI";
        public static final String DBTR_BIC = "//doc:DbtrAgt//doc:Cd";

        public static final String CREDITOR_NAME = "//doc:Cdtr/doc:Nm";
        public static final String CDTR_BICFI = "//doc:CdtrAgt//doc:BICFI";
        public static final String CDTR_BIC = "//doc:CdtrAgt//doc:Cd";

        public static final String CHARGE_BEARER_PMTINF = "//doc:PmtInf/doc:ChrgBr";
        public static final String CHARGE_BEARER_TX = "//doc:CdtTrfTxInf/doc:ChrgBr";

        public static final String DEBTOR_ADDRESS_ROOT = "//doc:Dbtr/doc:PstlAdr";
        public static final String CREDITOR_ADDRESS_ROOT = "//doc:Cdtr/doc:PstlAdr";

        public static final String[] AMOUNT_VALUE_PATHS = {
                        AMOUNT_VALUE_INSTD,
                        AMOUNT_VALUE_INTERBANK
        };

        public static final String[] AMOUNT_CURRENCY_PATHS = {
                        AMOUNT_CCY_INSTD,
                        AMOUNT_CCY_INTERBANK
        };

        public static final String[] UETR_PATHS = {
                        UETR,
                        REFERENCE_END_TO_END_ID
        };

        public static final String[] CHARGE_BEARER_PATHS = {
                        CHARGE_BEARER_PMTINF,
                        CHARGE_BEARER_TX
        };

        private Pacs008Xpaths() {
                throw new AssertionError("Utility class should not be instantiated");
        }
}
