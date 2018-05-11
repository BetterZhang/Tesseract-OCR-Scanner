package com.zl.tesseract.scanner.domain;

import java.util.List;

/**
 * Created by Android Studio.
 * Author : zhangzhongqiang
 * Email  : betterzhang.dev@gmail.com
 * Time   : 2018/05/10 上午 9:18
 * Desc   : description
 */
public class ResponseBody {

    /**
     * error_msg : ok
     * lineNum : 23
     * error_code : 0
     * linesText : ["sto申通快递","标准","cpssr","express","快递","成都武侯J036","国成都市内包","吴余","13881967335","收四川省成都市武侯区武科东二路1号天际首府","鼎立车品","15988650245","寄","浙江省宁波市江北区榭嘉路386号三和嘉园1号","3320523994475","2016/12/17","快件送达收件人地址,经收件人或收件人(寄件人)","14:45:46|允许的代收人签字,视为送达。您的签字代表您已经","5/10","签收此包裹,并已确认商品信息无误、包装完好、没","有划痕、破损等表面质量问题。","打印时间","签收栏"]
     */

    private String error_msg;
    private int lineNum;
    private int error_code;
    private List<String> linesText;

    public String getError_msg() {
        return error_msg;
    }

    public void setError_msg(String error_msg) {
        this.error_msg = error_msg;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public int getError_code() {
        return error_code;
    }

    public void setError_code(int error_code) {
        this.error_code = error_code;
    }

    public List<String> getLinesText() {
        return linesText;
    }

    public void setLinesText(List<String> linesText) {
        this.linesText = linesText;
    }

    @Override
    public String toString() {
        return "ResponseBody{" +
                "error_msg='" + error_msg + '\'' +
                ", lineNum=" + lineNum +
                ", error_code=" + error_code +
                ", linesText=" + linesText +
                '}';
    }
}

