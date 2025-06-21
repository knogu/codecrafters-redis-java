public class SimpleString implements RespDataType {
    private final String string;

    public SimpleString(String string) {
        this.string = string;
    }

    @Override
    public String encode() {
        return '+' + string + "\r\n";
    }
}
