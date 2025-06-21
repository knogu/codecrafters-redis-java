public class BulkString implements RespDataType {
    private final String string;

    public BulkString(String string) {
        this.string = string;
    }

    @Override
    public String encode() {
        if (string == null) {
            return "$-1\r\n";
        } else {
            return "$" + string.length() + "\r\n" + string + "\r\n";
        }
    }
}
