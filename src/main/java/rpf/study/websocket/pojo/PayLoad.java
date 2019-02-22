package rpf.study.websocket.pojo;

public class PayLoad {
    /**
     * type mean
     * p    私人消息
     * sys  系统消息
     * g    群聊消息
     * on   上线通知
     * off  离线通知
     * error错误反馈
     * Apply群聊验证
     * **/
    public final static String SYS="SYS";
    public final static String PVP="P";
    public final static String PVG="G";
    public final static String ONLINE="ON";
    public final static String OFFLINE="OFF";
    public final static String ERROR="ERROR";
    public final static String GROUP_APPLY="APPLY";
    private String type;
    private String code;
    private Object data;

    public PayLoad(String type,Object data) {
        this.type = type;
        this.code = "200";
        this.data = data;
    }

    public PayLoad() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.code="200";
        this.data = data;
    }

    @Override
    public String toString() {
        return "{" +
                "type:'" + type + '\'' +
                ", code:'" + code + '\'' +
                ", data:" + data +
                '}';
    }
}
