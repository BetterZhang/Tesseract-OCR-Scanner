package com.zl.tesseract.scanner.domain;

public class MailAnalysisResult {

    /** 解析成功. */
    public static final int SUCCESS = 200;
    /** 解析失败. */
    public static final int ERROR = 500;
    /** 返回码，200为成功，其余均为失败，仅200时，其余数据项有效. */
    private int code = 200;

    /** 运单号. */
    private String mailNo = "";

    /** 收件人姓名. */
    private String recipientName = "";

    /** 收件人电话. */
    private String recipientPhone = "";

    /**
     * 构造方法.
     */
    public MailAnalysisResult() {
    }

    /**
     * 构造方法（失败的情况，cd = ERROR）.
     * @param cd Code
     */
    public MailAnalysisResult(int cd) {
        this.code = cd;
        this.mailNo = null;
        this.recipientName = null;
        this.recipientPhone = null;
    }

    /**
     * 构造方法（成功的情况， code = SUCCESS）.
     * @param mNo 运单号
     * @param rNm 收件人姓名
     * @param rPh 收件人电话
     */
    public MailAnalysisResult(String mNo, String rNm, String rPh) {
        this.code = SUCCESS;
        this.mailNo = null;
        this.recipientName = null;
        this.recipientPhone = null;
    }

    /**
     * 构造方法（自定义）.
     * @param cd Code
     * @param mNo 运单号
     * @param rNm 收件人姓名
     * @param rPh 收件人电话
     */
    public MailAnalysisResult(int cd, String mNo, String rNm, String rPh) {
        this.code = cd;
        this.mailNo = mNo;
        this.recipientName = rNm;
        this.recipientPhone = rPh;
    }

    /*
     * Getter And Setter 方法.
     */
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMailNo() {
        return mailNo;
    }

    public void setMailNo(String mailNo) {
        this.mailNo = mailNo;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    @Override
    public String toString() {
        return "code:" + code + "; mailNo:" + mailNo + "; recipientName:" + recipientName + "; recipientPhone:" + recipientPhone;
    }
}
